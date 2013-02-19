(ns cassiel.cubewar.test.tournament
  "Test tournament machinery."
  (:use clojure.test)
  (:require (cassiel.cubewar [cube :as c]
                             [players :as pl]
                             [state-navigation :as n]
                             [tournament :as t])))

(deftest test-fire-journal
  (testing "miss"
    (let [state (-> {}
                    (pl/add-player :P (pl/gen-player [0 0 0])))
          world {:cube state :scoring nil}]
      (is (= [{:to :P :type :miss}]
             (-> world
                 (t/fire :P)
                 (:journal))))))

  (testing "hit"
    (let [state (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))
          world {:cube state :scoring nil}]
      (is (= [{:to :P1 :type :hit :hit :P2}
              {:to :P2 :type :hit-by :hit-by :P1}]
             (-> world
                 (t/fire :P1)
                 (:journal)))))))
