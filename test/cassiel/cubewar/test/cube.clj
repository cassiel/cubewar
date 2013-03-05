;; Test cube operations.

(ns cassiel.cubewar.test.cube
  (:use clojure.test)
  (:require (cassiel.cubewar [manifest :as m]
                             [cube :as cube])))

(def tagged-cube
  (reduce
   (fn [m [k v]] (assoc m k v))
   {}
   (for [x (range m/CUBE-SIZE)
         y (range m/CUBE-SIZE)
         z (range m/CUBE-SIZE)]
     [[x y z] (symbol (str "C" x y z))])))

(deftest wall-detection
  (testing "walls"
    (is (not (cube/wall? [0 0 0])))
    (is (not (cube/wall? (repeat 3 (dec m/CUBE-SIZE)))))
    (is (cube/wall? [0 m/CUBE-SIZE 0]))
    (is (cube/wall? [0 0 -1]))
    )
  )

(deftest movement
  (testing "forward"
    (is (= [0 1 0]
           (cube/forward [0 0 0]))))

  (testing "pitch up"
    (is (= [0 0 0]
           (cube/pitch-up [0 0 0])))
    (is (= [0 0 1]
           (cube/pitch-up [0 1 0])))
    (is (= [0 -1 0]
           (cube/pitch-up [0 0 1]))))

  (testing "pitch down"
    (is (= [0 0 0]
           (cube/pitch-down [0 0 0])))
    (is (= [0 0 -1]
           (cube/pitch-down [0 1 0])))
    (is (= [0 1 0]
           (cube/pitch-down [0 0 1]))))

  (testing "roll left"
    (is (= [0 0 0]
           (cube/roll-left [0 0 0])))
    (is (= [0 1 0]
           (cube/roll-left [0 1 0])))
    (is (= [0 0 1]
           (cube/roll-left [1 0 0])))
    (is (= [-1 0 -1]
           (cube/roll-left [-1 0 1]))))

  (testing "roll right"
    (is (= [0 0 0]
           (cube/roll-right [0 0 0])))
    (is (= [0 1 0]
           (cube/roll-right [0 1 0])))
    (is (= [0 0 -1]
           (cube/roll-right [1 0 0])))
    (is (= [1 0 1]
           (cube/roll-right [-1 0 1]))))

  (testing "yaw left"
    (is (= [0 0 0]
           (cube/yaw-left [0 0 0])))
    (is (= [0 0 -1]
           (cube/yaw-left [0 0 -1])))
    (is (= [0 1 0]
           (cube/yaw-left [1 0 0])))
    (is (= [-1 0 1]
           (cube/yaw-left [0 1 1]))))

  (testing "yaw right"
    (is (= [0 0 0]
           (cube/yaw-right [0 0 0])))
    (is (= [0 0 -1]
           (cube/yaw-right [0 0 -1])))
    (is (= [0 -1 0]
           (cube/yaw-right [1 0 0])))
    (is (= [1 -1 0]
           (cube/yaw-right [1 1 0]))))

  (testing "composition"
    (is (= (tagged-cube [0 1 0])
           ((comp tagged-cube cube/forward) [0 0 0])))
    (is (= (tagged-cube [1 1 0])
           ((comp tagged-cube cube/forward cube/yaw-right) [0 1 0])))
    (is (= (tagged-cube [1 1 0])
           ((comp tagged-cube cube/forward cube/yaw-right cube/forward) [0 0 0])))))
