;; Test cube operations.

(ns cassiel.cubewar.test.players
  (:use clojure.test)
  (:require [cassiel.cubewar [players :as pl]]))

(deftest basics
  (testing "population 1"
    (let [p (pl/gen-player [1 0 0])]
      (is (= [1 1 0] (p [0 1 0]))))))

(deftest populate
  (testing "populate 1"
    (is (pl/add-player {} 'Player-1 (pl/gen-player [0 0 0]))))

  (testing "populate 2"
    (is
     (-> {}
         (pl/add-player 'Player-1 (pl/gen-player [0 0 0]))
         (pl/add-player 'Player-2 (pl/gen-player [1 0 0])))))

  (testing "name clash"
    (is (= "player already in cube"
           (try
             (-> {}
                 (pl/add-player 'Player-1 (pl/gen-player [0 0 0]))
                 (pl/add-player 'Player-1 (pl/gen-player [1 0 0])))
             (catch IllegalStateException exn (.getMessage exn))))
     )    )

  (testing "position clash"
    (is (= "cell already occupied"
           (try
             (-> {}
                 (pl/add-player 'Player-1 (pl/gen-player [0 0 0]))
                 (pl/add-player 'Player-2 (pl/gen-player [0 0 0])))
             (catch IllegalStateException exn (.getMessage exn))))
     ))
  )
