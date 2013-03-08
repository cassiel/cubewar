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

(defn attach
  "Attaches a player (game state only; not networking). A new player is put into
   standby in the round scoring system, if not already present."
  [world name]
  (let [scoring (:scoring world)]
    (assoc world :scoring (assoc scoring name (or (scoring name) 0)))))

(defn detach
  "Remove a player completely from the game state. (No changes to networking.)"
  [world name]
  (let [world' (assoc world
                 :arena (dissoc (:arena world) name)
                 :scoring (dissoc (:scoring world) name))]
    (if (< (count (:arena world')) 2)
      ;; Reduce is slight overkill, but will work for > 1 active player.
      ;; (`if-let` would make more sense.)
      (reduce remove-from-arena world' (keys (:arena world')))
      world')))

(defn start-round
  "Start a new round: reset all scores, put all players into the arena.
   TODO: the population pass needs to be a bit more random."
  [world]
  (if (< (+ (count (:arena world))
            (count (:scoring world))) 2)
    (throw+ {:type ::NOT-ENOUGH-PLAYERS})
    (assoc world
      :arena (reduce (fn [a [name _]] (into-arena a name))
                     (:arena world)
                     (:scoring world))
      :scoring (reduce (fn [s [name _]] (assoc s name m/START-SCORE))
                       (:scoring world)
                       (:scoring world)))))

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
          (assoc world
            :journal [{:to name :action :error :args {:message "not currently in play"}}])

          :else
          (let [victim (v/fire arena me-playing)]
            (if victim
              (let [old-score (get scoring victim)]
                (if old-score
                  (let [new-score (dec old-score)
                        j1 {:to name :action :hit :args {:player victim}}
                        j2 {:to victim
                            :action :hit-by
                            :args {:player name :hit-points new-score}}]
                    ;; Re-score the victim, remove from arena if hit-points now zero.
                    (assoc world
                      :scoring (assoc scoring victim new-score)
                      :arena (if (pos? new-score) arena (dissoc arena victim))
                      :journal (if (pos? new-score)
                                 [j1 j2]
                                 ;; TODO broadcast messages.
                                 [j1 j2 {:to m/BROADCAST
                                         :action :dead
                                         :args {:player victim}}])))
                  (throw+ {:type ::NOT-IN-SYSTEM :player victim})))

              (assoc world
                :journal [{:to name :action :miss}]))))))

(defn move
  "Perform a cube move. We report `:blocked` or a new view."
  [world name action]
  (let [{:keys [arena]} world
        f (c/manoeuvres action)]
    (if f
      (try+
        (let [arena' (n/navigate arena name f)
              me (get arena' name)
              view (v/look-plane arena' me)]
          (assoc world
            :arena arena'
            :journal [{:to name
                       :action :view
                       :args (assoc (v/dict-format view) :manoeuvre action)}]))
        (catch
            [:type ::n/NOT-EMPTY]
            _
          (assoc world :journal [{:to name :action :blocked}]))

        (catch
            [:type ::n/NOT-IN-PLAY]
            _
          (assoc world :journal [{:to name
                                  :action :error
                                  :args {:message "not currently in play"}}])))

      (throw+ {:type ::BAD-ACTION :action action}))))
