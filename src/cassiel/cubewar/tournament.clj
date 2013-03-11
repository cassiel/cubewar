(ns cassiel.cubewar.tournament
  "Central game-play, with hit points and rounds. The functions here correspond to
   actual OSC commands coming in from the clients (after appropriate authentication).
   `tournament` also builds journal lists of actions to transmit or broadcast."
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as c]
                             [players :as pl]
                             [view :as v]
                             [state-navigation :as n]))
  (:use [slingshot.slingshot :only [try+ throw+]]))

(defn journalise
  "Append a journal entry. (Doesn't remove old entries.)"
  [world & entries]
  (assoc world :journal
         (reduce (fn [j e] (conj (vec j) e))
                 (:journal world)
                 entries)))

(defn broadcast-alert
  [msg]
  {:to m/BROADCAST :action :alert :args {:message msg}})

(defn occupied [arena pos]
  (some (fn [[_ pos-fn]] (= pos (pos-fn [0 0 0]))) arena))

(defn find-space
  "Find an empty location in a map of players."
  [arena]
  (some #(when-not (occupied arena %) %)
        (for [x (range m/CUBE-SIZE)
              y (range m/CUBE-SIZE)
              z (range m/CUBE-SIZE)]
          [x y z])))

(defn- into-arena
  [arena name]
  (assoc arena name (or (arena name)
                        (pl/gen-player (find-space arena)))))

(defn- remove-from-arena
  "Remove a player from the arena."
  [world p]
  (assoc world
    :arena (dissoc (:arena world) p)))

(defn transmit-all-views
  "For each active player, send it its own (new) view.

   Optional `arg-modifier` takes args and name, and returns an args, possibly altered.
   (Used for adding `:manoeuvre` information.)"
  [world arg-modifier action]

  (reduce (fn [w name]
            (let [a (:arena world)
                  me (get a name)
                  args (v/dict-format (v/look-plane a me))
                  args' (if arg-modifier (arg-modifier args name) args)]
              (journalise w {:to name
                             :action action
                             :args args'})))
          world
          ;; We reduce over the keys so that we can sort them for unit-testing.
          (sort (keys (:arena world)))))

(defn start-round
  "Start a new round: reset all scores, put all players into the arena.
   TODO: the population pass needs to be a bit more random."
  [world]
  (if (< (+ (count (:arena world))
            (count (:scoring world))) m/MIN-IN-PLAY)
    (throw+ {:type ::NOT-ENOUGH-PLAYERS})
    (transmit-all-views
     (assoc world
       :arena (reduce (fn [a [name _]] (into-arena a name))
                      (:arena world)
                      (:scoring world))
       :scoring (reduce (fn [s [name _]] (assoc s name m/START-SCORE))
                        (:scoring world)
                        (:scoring world)))
     nil
     :start-round)))

(defn check-for-new-round
  "See whether we have an empty arena, and can start a new round (which may
   fail if we don't have enough players waiting)."
  [world]
  (if (empty? (:arena world))
    (try+
     (start-round world)
     (catch
         [:type ::NOT-ENOUGH-PLAYERS]
         _
       world))

    world))

(defn attach
  "Attaches a player (game state only; not networking). A new player is put into
   standby in the round scoring system, if not already present."
  [world name]
  (let [scoring (:scoring world)]
    (check-for-new-round
     (journalise
      (assoc world
        :scoring (assoc scoring name (or (scoring name) 0)))
      {:to name :action :welcome}))))

(defn detach
  "Remove a player completely from the game state. (No changes to networking.)"
  [world name]
  (let [world' (assoc world
                 :arena (dissoc (:arena world) name)
                 :scoring (dissoc (:scoring world) name))]
    (if (< (count (:arena world')) m/MIN-IN-PLAY)
      ;; Reduce is slight overkill, but will work for > 1 active player.
      ;; (`if-let` would make more sense.)
      (journalise
       (reduce remove-from-arena world' (keys (:arena world')))
       {:to m/BROADCAST :action :end-round}
       (broadcast-alert "round over (no winner)"))
      world')))

(defn fire
  "Takes, and returns, a complete world state. Also returns a journal.
   If a player is not in the scoring system, it's an internal error.
   If the player is not in the cube, then they're (presumably) sitting
   out this tournament round."
  [world name]
  (let [{:keys [arena scoring]} world
        me-scored (get scoring name)
        me-playing (get arena name)]

    (cond (nil? me-scored)
          (throw+ {:type ::NOT-IN-SYSTEM :player name})

          (nil? me-playing)
          (journalise world
                      {:to name :action :error :args {:message "not currently in play"}})

          :else
          (let [victim (v/fire arena me-playing)]
            (if victim
              (let [old-score (get scoring victim)]
                (if old-score
                  (let [new-score (dec old-score)
                        ;; Report the hit (to shooter and to victim):
                        world
                        (journalise world
                                    {:to name :action :hit :args {:player victim}}
                                    {:to victim
                                     :action :hit-by
                                     :args {:player name :hit-points new-score}})


                        ;; If player killed, broadcast it:
                        world
                        (if-not (pos? new-score)
                          (journalise world {:to m/BROADCAST
                                             :action :dead
                                             :args {:player victim}})
                          world)

                        ;; Remove player from arena if dead:
                        arena (if (pos? new-score) arena (dissoc arena victim))

                        ;; Empty arena (and report game end) if last player:
                        last-player? (= (count arena) 1)
                        arena (if last-player? {} arena)

                        world (if last-player?
                                (journalise world
                                            {:to m/BROADCAST :action :end-round}
                                            (broadcast-alert (str "round over, winner " name)))
                                world)]

                    ;; TODO: we probably shouldn't do this immediately.
                    (check-for-new-round
                     (assoc world
                       :scoring (assoc scoring victim new-score)
                       :arena arena)))

                  (throw+ {:type ::NOT-IN-SYSTEM :player victim})))

              (journalise world {:to name :action :miss}))))))

(def position-altering #{:forward})

(defn move
  "Perform a cube move. We report `:blocked` or a new view. If we move,
   we actually have to send views to all active players, since they might
   be looking at the guy who's moving."
  [world name action]
  (let [{:keys [arena]} world
        f (c/manoeuvres action)]
    (if f
      (try+
        (let [arena' (n/navigate arena name f)
              me (get arena' name)
              view (v/look-plane arena' me)
              world' (assoc world :arena arena')
              ]
          (if (position-altering action)
            ;; Send appropriate new view to all players:
            #_ (transmit-all-move-views world' name)
            (transmit-all-views
             world'
             (fn [args n] (if (= n name)
                           (assoc args :manoeuvre :forward)
                           args))
             :view)
            ;; Only affects me:
            (journalise world' {:to name
                                :action :view
                                :args (assoc (v/dict-format view) :manoeuvre action)})))
        (catch
            [:type ::n/NOT-EMPTY]
            _
          (journalise world {:to name :action :blocked}))

        (catch
            [:type ::n/NOT-IN-PLAY]
            _
          (journalise world {:to name
                             :action :error
                             :args {:message "not currently in play"}})))

      (throw+ {:type ::BAD-ACTION :action action}))))
