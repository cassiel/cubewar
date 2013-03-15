(ns cassiel.cubewar.view
  "Generate view from a player's perspective. All take player orientation functions
   (not names)."
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as c]
                             [players :as pl])))

(defn look
  "Examine a cell coordinate from a player's perspective. The player will only see
   itself at [0 0 0] if it's in the arena.
   Possible results: :empty, :wall, {:player {:name name}}."
  [arena me pos]
  (let [abs-pos (me pos)]
    (if (c/wall? abs-pos)
      :wall
      (let [p (pl/player-at arena abs-pos)]
        (if p
          {:player {:name p}}
          :empty)))))

(defn look-plane
  "Return the transverse plane view: a sequence of ascending Y sweeps,
   ordered from low to high X.
   TODO: we may have to start thinking about the performance of look()."
  [arena me]
  (for [x (range (int (/ (- m/VIEW-WIDTH) 2))
                 (inc (int (/ m/VIEW-WIDTH 2))))]
    (for [y (range m/VIEW-DEPTH)]
      (look arena me [x y 0]))))

(defn look-arena
  "Full arena view, fixed reference frame. Outermost sweep is Z (low to high),
   then X (left to right), then Y (back to front)"
  [arena]
  (for [z (range m/VIEW-DEPTH)]
    (for [x (range m/VIEW-DEPTH)]
      (for [y (range m/VIEW-DEPTH)]
        (look arena identity [x y z])))))

(defn look-ahead
  "Return a sequence for the view directly ahead (including our origin), as far
   as the fire range."
  [arena me]
  (for [y (range m/FIRE-DEPTH)]
    (look arena me [0 y 0])))

(defn fire
  "A named player fires a shot. Return nil or player map."
  [arena me]
  (let [view (rest (look-ahead arena me))]
    ;; Look for first non-`:empty`, non-`:wall`, if any.
    (some #(when-not (#{:empty :wall} %) (:player %)) view)))

(defn ordinal-keys [prefix items]
  (first
   (reduce (fn [[m i] x] [(assoc m (keyword (str prefix i)) x) (inc i)])
           [{} 0]
           items)))

(defn dict-format
  "Turn a view (a list along X of successive Y vectors) into a map structure. MaxMSP
   can't handle dictionaries where a data entry is itself a list of dictionaries, so
   we have to synthesise distinct ordinal-looking keys."
  [view]
  (ordinal-keys "x" (map (partial ordinal-keys "y") view)))

(defn dict-format-3D
  "As for the 2D version, but with an out iteration over Z."
  [view]
  (ordinal-keys "z" (map dict-format view)))
