(ns cassiel.cubewar.test.view
  "Players' view generation."
  (:use clojure.test)
  (:require (cassiel.cubewar [cube :as c]
                             [players :as pl]
                             [view :as v]
                             [state-navigation :as n])))

(deftest basics
  (testing "empty view"
    (let [p (pl/gen-player [0 0 0])
          state (pl/add-player {} 'me p)]
      (is (= :empty
             (v/look state p [0 1 0])))))

  (testing "can see self"
    (let [p (pl/gen-player [0 0 0])
          state (pl/add-player {} 'me p)]
      (is (= {:player 'me}
             (v/look state p [0 0 0])))))

  (testing "can see enemy ahead"
    (let [me (pl/gen-player [0 0 0])
          other (pl/gen-player [0 1 0])
          state (-> {}
                    (pl/add-player 'me me)
                    (pl/add-player 'other other))]
      (is (= {:player 'other}
             (v/look state me [0 1 0]))))))

(deftest plane-view
  (testing "initial view"
    (let [p0 (pl/gen-player [0 0 0])
          state (-> {} (pl/add-player 'me p0))]
      (is (= [(repeat 3 :wall)
              [{:player 'me} :empty :empty]
              (repeat 3 :empty)]
             (v/look-plane state p0)))))

  (testing "complex view"
    (let [state (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [1 0 0]))
                    (pl/add-player :P3 (pl/gen-player [0 1 0])))]
      (is (= [[{:player :P1} {:player :P3} :empty]
              [{:player :P2} :empty :empty]
              (repeat 3 :empty)]
             (v/look-plane state (pl/gen-player [1 0 0])))))))

(deftest fire-view
  (testing "initial fire view"
    (is (= (repeat 3 :empty)
           (v/look-ahead {} (pl/gen-player [0 0 0])))))

  (testing "wall view"
    (is (= [:empty :wall :wall]
           (v/look-ahead {} (pl/gen-player [0 2 0])))))

  (testing "complex fire view"
    (let [state (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [1 0 0]))
                    (pl/add-player :P3 (pl/gen-player [1 2 0])))]
      (is (= [{:player :P2} :empty {:player :P3}]
             (v/look-ahead state (pl/gen-player [1 0 0])))))))

(deftest test-firings
  (testing "miss"
    (let [state (pl/add-player {} :PLAYER (pl/gen-player [0 0 0]))]
      (is (nil? (v/fire state :PLAYER)))))

  (testing "hit"
    (let [state (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))]
      (is (= :P2 (v/fire state :P1)))))

  (testing "hit further"
    (let [state (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 2 0])))]
      (is (= :P2 (v/fire state :P1)))))

  (testing "hit nearest"
    (let [state (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0]))
                    (pl/add-player :P3 (pl/gen-player [0 2 0])))]
      (is (= :P2 (v/fire state :P1)))))

  (testing "turn to hit"
    (let [state (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [1 0 0])))]
      (is (= :P2
             (-> state
                 (n/navigate :P1 c/yaw-right)
                 (v/fire :P1)))))))
