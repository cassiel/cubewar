(ns cassiel.cubewar.test.view
  "Players' view generation."
  (:use clojure.test)
  (:require (cassiel.cubewar [cube :as c]
                             [players :as pl]
                             [view :as v])))

(deftest basics
  (testing "empty view"
    (let [p (pl/gen-player [0 0 0])
          state (pl/add-player {} 'me p)]
      (is (= :empty
             (v/look state p [0 1 0])))))

  (testing "can see self"
    (let [p (pl/gen-player [0 0 0])
          state (pl/add-player {} 'me p)]
      (is (= [:player 'me]
             (v/look state p [0 0 0])))))

  (testing "can see enemy ahead"
    (let [me (pl/gen-player [0 0 0])
          other (pl/gen-player [0 1 0])
          state (-> {}
                    (pl/add-player 'me me)
                    (pl/add-player 'other other))]
      (is (= [:player 'other]
             (v/look state me [0 1 0])))))


    )
