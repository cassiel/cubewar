(ns user
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as cube]
                             [players :as pl]
                             [view :as v]
                             [state-navigation :as nav]
                             [tournament :as t]
                             [network :as net]
                             [server :as srv])
            :reload-all)
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


(def WORLD-STATE (atom world-n))

@WORLD-STATE


(srv/serve1 WORLD-STATE nil {:host "localhost" :port 9999} :attach [:P1 9998])

(srv/serve1 WORLD-STATE nil {:host "localhost" :port 9999} :handshake nil)

(srv/serve1 WORLD-STATE nil {:host "localhost" :port 9999} :fire nil)

(srv/serve1 WORLD-STATE nil {:host "localhost" :port 9999} :detach nil)

;; --- Network testing.

(def g (srv/start-game "Cubewar-Lein" 8123 "cubewar"))

(srv/examine g)

(srv/clear g)

(map (fn [[name f]] {:n name :pos (f [0 0 0])})
     (:arena (srv/examine g)))

(srv/interact g 8001 :attach ["Harmony" 9998])
(srv/interact g 8001 :handshake nil)
(srv/interact g 8001 :fire nil)
(srv/interact g 8001 :kick nil)
(srv/interact g 8001 :pitch-up nil)
(srv/interact g 8001 :yaw-right nil)
(srv/interact g 8001 :forward nil)
(srv/interact g 8001 :detach nil)

(srv/interact g 8002 :attach ["Symphony" 9997])
(srv/interact g 8002 :handshake nil)
(srv/interact g 8002 :fire nil)
(srv/interact g 8002 :pitch-up nil)
(srv/interact g 8002 :yaw-right nil)
(srv/interact g 8002 :forward nil)
(srv/interact g 8002 :detach nil)

(srv/interact g 8003 :attach ["Observer" 9996])
(srv/interact g 8003 :handshake nil)
(srv/interact g 8003 :fire nil)
(srv/interact g 8003 :pitch-up nil)
(srv/interact g 8003 :yaw-right nil)
(srv/interact g 8003 :forward nil)
(srv/interact g 8003 :detach nil)

(srv/close g)

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

(v/dict-format [[1 2 3] [4 5 {:player 6}] [7 8 9]])

(v/dict-format-3D [[[1 2 3] [4 5 6] [7 8 9]]
                   [[10 11 12] [13 14 15] [16 17 18]]
                   [[19 20 21] [22 23 24] [25 26 27]]])

(first
 (reduce (fn [[m i] x] [(assoc m (keyword (str "P" i)) x) (inc i)])
         [{} 0]
         [[1 2 3] [4 5 6] [7 8 9]]))

(defn ordinal-keys [prefix items]
  (first
   (reduce (fn [[m i] x] [(assoc m (keyword (str prefix i)) x) (inc i)])
         [{} 0]
         items)))

(ordinal-keys "x" [1 2 3 4 5])

(def foo {:A [0 0 0] :B [0 0 1]})

(for [x (range m/CUBE-SIZE)
      y (range m/CUBE-SIZE)
      z (range m/CUBE-SIZE)]
  [x y z])

(defn occupied [arena pos]
  (some (fn [[_ pos-fn]] (= pos (pos-fn [0 0 0]))) arena))


(some #(when-not (occupied {:P identity :Q (fn [[x y z]] [x y (inc z)])} %) %)
      (for [x (range m/CUBE-SIZE)
            y (range m/CUBE-SIZE)
            z (range m/CUBE-SIZE)]
        [x y z])
      )

(t/start-round {:arena {}
                :scoring {:P1 1 :P2 3}})

(str ::pl/ALREADY-PRESENT)

(str ::junk/junk)

:cassiel.cubewar.players/ALREADY-PRESENT

(= ::pl/ALREADY-PRESENT :pl/ALREADY-PRESENT)
(= ::pl/ALREADY-PRESENT :cassiel.cubewar.players/ALREADY-PRESENT)

(keys
 (System/getenv))

(keys
 (System/getProperties))

(get
 (System/getProperties)
 "cubewar.version")

(get
 (System/getProperties)
 "os.name")

(System/getProperty "os.version")

(conj nil "A")

(#{:A} :B)

(sort [:B :A])

;; --- MD5


(org.apache.commons.codec.digest.DigestUtils/md5Hex "Hello")

16rA0

java.io.File/separator

(last [1 2 3])


({"A" 1 :B 2} "A")



((fn [[a b c]] [a b c]) [1 2])


(assoc {:A 1} :B 2 :C 3)

(str \-)

(apply str (reverse (cons 55 (cons \space (repeat 10 ">")))))

(format "%d %s"
        55
        (apply str (repeat 55 \>)))


world-n


(apply str (repeat (get (:scoring world-n) :P3) "-"))

(:scoring world-n)

(str (java.util.Date.))
