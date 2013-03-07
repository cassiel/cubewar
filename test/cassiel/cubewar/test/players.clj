(ns cassiel.cubewar.test.players
  "Test basic player set operations."
  (:use clojure.test
        slingshot.test)
  (:require (cassiel.cubewar [players :as pl])))

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
    (is (thrown+? [:type ::pl/ALREADY-PRESENT]
          (-> {}
              (pl/add-player 'Player-1 (pl/gen-player [0 0 0]))
              (pl/add-player 'Player-1 (pl/gen-player [1 0 0]))))))

  (testing "position clash"
    (is (thrown+? [:type ::pl/NOT-EMPTY]
          (-> {}
              (pl/add-player 'Player-1 (pl/gen-player [0 0 0]))
              (pl/add-player 'Player-2 (pl/gen-player [0 0 0])))))))
