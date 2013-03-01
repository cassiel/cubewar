(ns user
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as cube]
                             [players :as pl]
                             [view :as v]
                             [state-navigation :as nav]
                             [tournament :as t]
                             [network :as net]
                             [server :as srv]))
  (:import [net.loadbang.osc.data Message]))

(
 (reduce
  (fn [m [k v]] (assoc m k v))
  {}
  (for [x (range m/CUBE-SIZE)
        y (range m/CUBE-SIZE)
        z (range m/CUBE-SIZE)]
    [[x y z] (keyword (str "C" (+ x (* m/CUBE-SIZE (+ y (* m/CUBE-SIZE z))))))]
    ))
 [0 1 0])

(cube/inertial-cube [0 0 0])

(cube/forward [0 0 0])

(cube/pitch-up [0 1 0])

((comp cube/inertial-cube cube/forward cube/yaw-right) [0 0 0])

(map #(% [0 0 0]) (vals {'PLAYER (fn [[x y z]] :X)}))

(def state0
  (pl/add-player {} 'PLAYER (pl/gen-player [0 0 0])))

(def state1
  (pl/add-player {} 'PLAYER (comp (pl/gen-player [0 0 0]) cube/forward)))

(def state-n
  (-> {}
      (pl/add-player :P1 (pl/gen-player [0 0 0]))
      (pl/add-player :P2 (pl/gen-player [1 0 0]))
      (pl/add-player :P3 (pl/gen-player [0 1 0]))))

(v/look state0 (pl/gen-player [0 0 0]) [0 0 0])

(v/look state0 (comp (pl/gen-player [0 0 0]) cube/forward) [0 0 0])
(v/look state0 (comp (pl/gen-player [0 0 0]) cube/pitch-up) [0 0 0])

(v/look-plane state-n (pl/gen-player [0 0 0]))

(map (fn [[name f]] {:name name :pos (f [0 0 0])})
     state1)

state0

(v/fire state-n :P1)

(def world-n {:arena state-n
              :scoring {:P1 50 :P3 50}})

(t/fire world-n :P1)

(:journal
 (t/fire world-n :P1))

(
 (get
  (nav/navigate
   state0
   'PLAYER
   cube/forward
   )
  'PLAYER)
 [0 0 0])

(#{1 2} 1)

(range -10 5)

(int (/ (- 9) 2))

(and "A" 5)

(= {:A 1 :B 2} {:B 2 :A 1})

;; Networking.

(defn pr-message
  [^Message m]
  (println (.getAddress m))
  (doseq [i (range (.getNumArguments m))] (println "->" (.getValue (.getArgument m i)))))


(def r (net/start-receiver 8123 pr-message))

(.close r)

(.getPort r)

(get (re-find #"^/?(.+)+$" "/fooble") 1)

;; Testing server functions.

(srv/serve1 (atom {:world {} :journal []})
            :handshake []
            )

(srv/serve1 (atom {:world {:arena {} :scoring {:P 10}} :journal []})
            :fire [:P]
            )

;; State testing.

(def state-n
  (-> {}
      (pl/add-player :P1 (pl/gen-player [0 0 0]))
      (pl/add-player :P2 (pl/gen-player [1 0 0]))
      (pl/add-player :P3 (pl/gen-player [0 1 0]))))

(def world-n {:arena state-n
              :scoring {:P1 50 :P3 50}
              :origins->names {}
              :names->transmitters {}})


(def WORLD-STATE (atom {:world world-n :journal []}))

(srv/serve1 WORLD-STATE {:host "localhost" :port 9999} :attach [:P1 9998])

(srv/serve1 WORLD-STATE {:host "localhost" :port 9999} :handshake nil)

(srv/serve1 WORLD-STATE {:host "localhost" :port 9999} :fire nil)
(srv/serve1 WORLD-STATE :fire [:P3])
(srv/serve1 WORLD-STATE :pitch-up [:P1])
(srv/serve1 WORLD-STATE :yaw-right [:P1])


;; --- Network testing.

(def rs (srv/start-game 8123))

(:receiver rs)

@(:world rs)

(.close (:receiver rs))

;; --- Junk.

(str :A)
(clojure.string/replace :A ":" "/")
(class 4)
(isa? (class 4) Long)
(instance? Long 4)
(instance? clojure.lang.Keyword :A)
(keyword (gensym))
(class :A)
(clojure.set/map-invert {:A 1})

(first [1 2 3 5])

(def aaa :A)
(case aaa :A 1 2)

(or nil (throw (IllegalArgumentException. "A")))

(let [msg (net/make-message :ACTION {:A 3 :B 5})]
  (map #(.getValue (.getArgument msg %))
       (range (.getNumArguments msg))))

(letfn [(decomp [msg] (map #(.getValue (.getArgument msg %))
                               (range (.getNumArguments msg))))]
  (= ["A" ":" 3 "B" ":" 5]
     (decomp (net/make-message :BOGUS {:A 3 :B 5}))))

(let [msg (net/make-message (Message. "/A") {:Foo [4 3 {:B 3}]})]
  (map #(.getValue (.getArgument msg %))
       (range (.getNumArguments msg))))

(instance? clojure.lang.Seqable [2])

(class [2])

(instance? java.util.Map [2])

(instance? java.util.List {:A 3})

(class {:A 3})

(doseq [a [2 3 4]] (println a))
