(ns cassiel.cubewar.test.tournament
  "Test tournament machinery."
  (:use clojure.test)
  (:require (cassiel.cubewar [players :as pl]
                             [tournament :as t])))

(deftest test-firings
  (testing "miss"
    (let [state (pl/add-player {} :PLAYER (pl/gen-player [0 0 0]))]
      (is (= :miss (t/fire state :PLAYER)))))

  (testing "hit"
    (let [state (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))]
      (is (= [:player :P2] (t/fire state :P1))))))

(deftest test-hit-points)
