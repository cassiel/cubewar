(ns cassiel.cubewar.test.view
  "Players' view generation."
  (:use clojure.test)
  (:require (cassiel.cubewar [cube :as c]
                             [players :as pl]
                             [view :as v]
                             [state-navigation :as n])))

(deftest formatting
  (testing "view formatting (2D)"
    (is (= {:x0 {:y0 1 :y1 2 :y2 3}
            :x1 {:y0 4 :y1 5 :y2 6}
            :x2 {:y0 7 :y1 8 :y2 9}}
           (v/dict-format [[1 2 3] [4 5 6] [7 8 9]]))))

  (testing "view formatting (3D)"
    (is (= {:z0 {:x0 {:y0  1 :y1  2 :y2  3}
                 :x1 {:y0  4 :y1  5 :y2  6}
                 :x2 {:y0  7 :y1  8 :y2  9}}
            :z1 {:x0 {:y0 10 :y1 11 :y2 12}
                 :x1 {:y0 13 :y1 14 :y2 15}
                 :x2 {:y0 16 :y1 17 :y2 18}}
            :z2 {:x0 {:y0 19 :y1 20 :y2 21}
                 :x1 {:y0 22 :y1 23 :y2 24}
                 :x2 {:y0 25 :y1 26 :y2 27}}}
           (v/dict-format-3D [[[1 2 3] [4 5 6] [7 8 9]]
                              [[10 11 12] [13 14 15] [16 17 18]]
                              [[19 20 21] [22 23 24] [25 26 27]]])))))

(deftest basics
  (testing "empty view"
    (let [p (pl/gen-player [0 0 0])
          arena (pl/add-player {} 'me p)]
      (is (= :empty
             (v/look arena p [0 1 0])))))

  (testing "can see self"
    (let [p (pl/gen-player [0 0 0])
          arena (pl/add-player {} 'me p)]
      (is (= {:player {:name 'me}}
             (v/look arena p [0 0 0])))))

  (testing "can see enemy ahead"
    (let [me (pl/gen-player [0 0 0])
          other (pl/gen-player [0 1 0])
          arena (-> {}
                    (pl/add-player 'me me)
                    (pl/add-player 'other other))]
      (is (= {:player {:name 'other}}
             (v/look arena me [0 1 0]))))))

(deftest plane-view
  (testing "initial view"
    (let [p0 (pl/gen-player [0 0 0])
          arena (-> {} (pl/add-player 'me p0))]
      (is (= [(repeat 3 :wall)
              [{:player {:name 'me}} :empty :empty]
              (repeat 3 :empty)]
             (v/look-plane arena p0)))))

  (testing "complex view"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [1 0 0]))
                    (pl/add-player :P3 (pl/gen-player [0 1 0])))]
      (is (= [[{:player {:name :P1}} {:player {:name :P3}} :empty]
              [{:player {:name :P2}} :empty :empty]
              (repeat 3 :empty)]
             (v/look-plane arena (pl/gen-player [1 0 0])))))))

(deftest arena-view
  (testing "empty arena"
    (is (= (repeat 3 (repeat 3 (repeat 3 :empty)))
           (v/look-arena {}))))

  (testing "player in centre"
    ;; For hysterical reasons: outermost loop is Z (low to high), then
    ;; X (left to right), then Y (back to front).
    (let [arena (pl/add-player {} :P1 (pl/gen-player [1 1 1]))]
      (is (= [#_ Z0 (repeat 3 (repeat 3 :empty))
              #_ Z1 [(repeat 3 :empty)
                     [:empty {:player {:name :P1}} :empty]
                     (repeat 3 :empty)]
              #_ Z2 (repeat 3 (repeat 3 :empty))]
             (v/look-arena arena)))))

  (testing "player at origin"
    (let [arena (pl/add-player {} :P1 (pl/gen-player [0 0 0]))]
      (is (= [#_ Z0 [[{:player {:name :P1}} :empty :empty]
                     (repeat 3 :empty)
                     (repeat 3 :empty)]
              #_ Z1 (repeat 3 (repeat 3 :empty))
              #_ Z2 (repeat 3 (repeat 3 :empty))]
             (v/look-arena arena))))))

(deftest fire-view
  (testing "initial fire view"
    (is (= (repeat 3 :empty)
           (v/look-ahead {} (pl/gen-player [0 0 0])))))

  (testing "wall view"
    (is (= [:empty :wall :wall]
           (v/look-ahead {} (pl/gen-player [0 2 0])))))

  (testing "complex fire view"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [1 0 0]))
                    (pl/add-player :P3 (pl/gen-player [1 2 0])))]
      (is (= [{:player {:name :P2}} :empty {:player {:name :P3}}]
             (v/look-ahead arena (pl/gen-player [1 0 0])))))))

(deftest test-firings
  (testing "miss"
    (let [p (pl/gen-player [0 0 0])
          arena (pl/add-player {} :PLAYER p)]
      (is (nil? (v/fire arena p)))))

  (testing "hit"
    (let [p (pl/gen-player [0 0 0])
          arena (-> {}
                    (pl/add-player :P1 p)
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))]
      (is (= {:name :P2} (v/fire arena p)))))

  (testing "hit further"
    (let [p (pl/gen-player [0 0 0])
          arena (-> {}
                    (pl/add-player :P1 p)
                    (pl/add-player :P2 (pl/gen-player [0 2 0])))]
      (is (= {:name :P2} (v/fire arena p)))))

  (testing "hit nearest"
    (let [p (pl/gen-player [0 0 0])
          arena (-> {}
                    (pl/add-player :P1 p)
                    (pl/add-player :P2 (pl/gen-player [0 1 0]))
                    (pl/add-player :P3 (pl/gen-player [0 2 0])))]
      (is (= {:name :P2} (v/fire arena p)))))

  (testing "turn to hit"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [1 0 0])))
          arena' (n/navigate arena :P1 c/yaw-right)]
      (is (= {:name :P2}
             (v/fire arena' (get arena' :P1)))))))
