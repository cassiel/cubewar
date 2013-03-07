(ns cassiel.cubewar.test.state-navigation
  "Test basic player navigation in the state."
  (:use clojure.test
        slingshot.test)
  (:require (cassiel.cubewar [cube :as c]
                             [players :as pl]
                             [state-navigation :as nav])))

(deftest ok-moves
  (testing "OK after forward"
    (let [state0 (pl/add-player {} :PLAYER (pl/gen-player [0 0 0]))
          state1 (nav/navigate state0 :PLAYER c/forward)]
      (is (= [0 1 0]
             ((:PLAYER state1) [0 0 0])))))

  (testing "OK after in-place move"
    (let [state0 (pl/add-player {} :PLAYER (pl/gen-player [0 0 0]))
          state1 (nav/navigate state0 :PLAYER c/pitch-up)]
      (is (= [0 0 0]
             ((:PLAYER state1) [0 0 0]))))))

(deftest bad-moves
  (testing "collision check with wall"
    (let [state (pl/add-player {} :PLAYER (pl/gen-player [0 2 0]))]
      (is (thrown+? [:type ::nav/NOT-EMPTY :contents :wall]
                    (nav/navigate state :PLAYER c/forward)))))

  (testing "collision check with second player"
    (let [state (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))]
      (is (thrown+? #(and (= (:type %) ::nav/NOT-EMPTY)
                          (:player (:contents %)))
                    (nav/navigate state :P1 c/forward)))))

  (testing "collision check with wall after double yaw"
    (let [state (-> {}
                    (pl/add-player :P (pl/gen-player [0 0 0]))
                    (nav/navigate :P c/yaw-left)
                    (nav/navigate :P c/yaw-left))]
      (is (thrown+? [:type ::nav/NOT-EMPTY :contents :wall]
            (nav/navigate state :P c/forward)))))

  (testing "collision check with second player after double pitch"
    (let [state (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0]))
                    (nav/navigate :P2 c/pitch-down)
                    (nav/navigate :P2 c/pitch-down))]
      (is (thrown+? #(and (= (:type %) ::nav/NOT-EMPTY)
                          (:player (:contents %)))
                    (nav/navigate state :P2 c/forward))))))
