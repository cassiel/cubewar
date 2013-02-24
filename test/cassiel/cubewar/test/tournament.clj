(ns cassiel.cubewar.test.tournament
  "Test tournament machinery."
  (:use clojure.test)
  (:require (cassiel.cubewar [cube :as c]
                             [players :as pl]
                             [state-navigation :as n]
                             [tournament :as t])))

(deftest fire-journal
  (testing "miss"
    (let [arena (-> {}
                    (pl/add-player :P (pl/gen-player [0 0 0])))
          world {:arena arena :scoring {:P 10}}]
      (is (= [{:to :P :action :miss}]
             (-> world
                 (t/fire :P)
                 (:journal))))))

  (testing "hit"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))
          world {:arena arena :scoring {:P1 10 :P2 10}}]
      (is (= [{:to :P1 :action :hit :args [:P2]}
              {:to :P2 :action :hit-by :args [:P1 9]}]
             (-> world
                 (t/fire :P1)
                 (:journal)))))))

(deftest sanity-check
  (testing "rogue in arena"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))
          world {:arena arena :scoring {:P1 10}}]

      (is (thrown-with-msg? IllegalStateException #"player not in scoring system: :P2"
            (-> world (t/fire :P1)))))))

(deftest move-journal
  (testing "forward OK"
    (let [arena (-> {}
                    (pl/add-player :P (pl/gen-player [0 0 0])))
          world {:arena arena :scoring nil}]
      (is (= [{:to :P :action :view :args [(repeat 3 :wall)
                                           [{:player :P} :empty :wall]
                                           [:empty :empty :wall]]}]
             (-> world
                 (t/move :P c/forward)
                 (:journal))))))

  (testing "forward blocked"
    (let [arena (-> {}
                    (pl/add-player :P (pl/gen-player [0 2 0])))
          world {:arena arena :scoring nil}]
      (is (= [{:to :P :action :blocked}]
             (-> world
                 (t/move :P c/forward)
                 (:journal))))))

  (testing "yaw left"
    (let [arena (-> {}
                    (pl/add-player :P (pl/gen-player [0 0 0])))
          world {:arena arena :scoring nil}]
      (is (= [{:to :P :action :view :args [(repeat 3 :wall)
                                           [{:player :P} :wall :wall]
                                           [:empty :wall :wall]]}]
             (-> world
                 (t/move :P c/yaw-left)
                 (:journal)))))))

(deftest basic-round-scoring
  (testing "in scoring but not in round"
    ;; We can't really get an empty cube, but:
    (let [world {:arena {} :scoring {:P 10}}
          {j :journal} (t/fire world :P)]
      (is (= [{:to :P :action :error :args ["not currently in play"]}]
             j))))

  (testing "score hit"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))
          world0 {:arena arena :scoring {:P1 10 :P2 10}}
          {:keys [world journal]} (t/fire world0 :P1)]
      (is (= [{:to :P1 :action :hit :args [:P2]}
              {:to :P2 :action :hit-by :args [:P1 9]}]
             journal))
      (is (= 9 (:P2 (:scoring world))))))

  (testing "knockout"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))
          world0 {:arena arena :scoring {:P1 1 :P2 1}}
          {:keys [world journal]} (t/fire world0 :P1)]
      (is (= [{:to :P1 :action :hit :args [:P2]}
              {:to :P2 :action :hit-by :args [:P1 0]}
              {:to :* :action :dead :args [:P2]}]
             journal))
      (is (= 0 (-> world (:scoring) (:P2))))
      (is (-> world (:arena) (:P1)))
      (is (nil? (-> world (:arena) (:P2)))))))
