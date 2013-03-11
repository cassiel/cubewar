(ns cassiel.cubewar.test.tournament
  "Test tournament machinery."
  (:use clojure.test
        slingshot.test)
  (:require (cassiel.cubewar [manifest :as m]
                             [players :as pl]
                             [state-navigation :as n]
                             [tournament :as t])))

(deftest basics
  (testing "journalise"
    (is (= {:journal [{:foo 99}]}
           (t/journalise {} {:foo 99})))
    (is (= {:journal [{:to m/BROADCAST :action :alert :args {:message "BOO!"}}]}
           (t/journalise {} (t/broadcast-alert "BOO!"))))
    (is (= {:journal [1 2 3]}
           (t/journalise {} 1 2 3))))

  (testing "occupied"
    (is (t/occupied {:P (pl/gen-player [0 0 0])} [0 0 0]))
    (is (not (t/occupied {} [0 0 0])))
    (is (not (t/occupied {:P (pl/gen-player [0 0 1])} [0 0 0])))
    (is (not (t/occupied {:P (pl/gen-player [0 0 0])} [1 0 0]))))

  (testing "find-space"
    (is (t/find-space {}))
    (let [arena {:P1 identity}
          pos (t/find-space arena)]
      (is pos)
      (is (not= ((:P1 arena) [0 0 0])
                pos)))))

(deftest game-state
  (testing "cannot move if not in arena"
    (let [world {:arena {} :scoring {:P 0}}]
      (is (= [{:to :P :action :error :args {:message "not currently in play"}}]
             (-> world (t/move :P :forward) (:journal))))))

  (testing "cannot fire if not in arena"
    (let [world {:arena {} :scoring {:P 0}}]
      (is (= [{:to :P :action :error :args {:message "not currently in play"}}]
             (-> world (t/fire :P) (:journal))))))

  (testing "attach doesn't put player in arena"
    (let [world (-> {:arena {} :scoring {}}
                    (t/attach :P))]
      (is (:P (:scoring world)))
      (is (nil? (:P (:arena world))))))

  (testing "attach when in play/scoring doesn't overwrite"
    (let [arena (-> {}
                    (pl/add-player :P (pl/gen-player [0 0 0])))
          world (-> {:arena arena :scoring {:P 7}}
                    (t/attach :P))]
      (is (:P (:arena world)))
      (is (= 7 (:P (:scoring world))))))

  (testing "player in arena not moved on round start"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [2 2 2])))
          world (t/start-round {:arena arena :scoring {:P2 0}})]
      (is (-> world (:arena) (:P2)))
      (is (= [2 2 2]
             ((-> world (:arena) (:P1)) [0 0 0])))))

  (testing "round start puts two players into arena"
    (let [world (-> {:arena {} :scoring {:P1 0 :P2 0}}
                    (t/start-round))]
      (is (:P1 (:arena world)))
      (is (:P2 (:arena world)))))

  (testing "round start fails if unsufficient players"
    (let [world {:arena {:P1 (pl/gen-player [0 0 0])} :scoring {}}]
      (is (thrown+? [:type ::t/NOT-ENOUGH-PLAYERS]
                    (t/start-round world)))))

  ;; We shouldn't start a round unless the arena is empty.
  #_ (testing "can start round with one active player"
       (let [world {:arena {:P1 (pl/gen-player [0 0 0])}
                    :scoring {:P1 0 :P2 0}}]
         (is (= 2 (count (-> world (t/start-round) (:arena)))))))

  (testing "can start round manually, with journal generated"
    (let [world (t/start-round {:arena {}
                                :scoring {:P1 0 :P2 0}})]
      (is (= 2 (count (:arena world))))
      (is (= [{:to m/BROADCAST :action :start-round}]
             (:journal world)))))

  (testing "attach doesn't start a game when not enough players"
    (let [world {:arena {}
                 :scoring {}}
          world' (t/attach world :P2)]
      (is (empty? (:arena world')))))

  (testing "attach starts a round when enough players"
    (let [world {:arena {}
                 :scoring {:P1 0}}
          world' (t/attach world :P2)]
      (is (= [{:to :P2 :action :welcome}
              {:to m/BROADCAST :action :start-round}]
             (:journal world')))
      (is (= 2 (count (:arena world'))))))

  (testing "detach removes from arena and standby."
    (let [arena (-> {}
                    (pl/add-player :P (pl/gen-player [0 0 0])))
          world (t/detach {:arena arena :scoring {:P 10}} :P)]
      (is (nil? (:P (:arena world))))
      (is (nil? (:P (:scoring world))))))

  (testing "detach does not leave too few players in arena"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 0 1])))
          world (t/detach {:arena arena :scoring {:P1 10 :P2 10}} :P1)]
      (is (nil? (:P2 (:arena world))))
      (is (= [{:to m/BROADCAST :action :end-round}
              {:to m/BROADCAST :action :alert :args {:message "round over (no winner)"}}]
             (:journal world))))))

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
      (is (= [{:to :P1 :action :hit :args {:player :P2}}
              {:to :P2 :action :hit-by :args {:player :P1 :hit-points 9}}]
             (-> world
                 (t/fire :P1)
                 (:journal))))))
)

(deftest sanity-check
  (testing "rogue in arena"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))
          world {:arena arena :scoring {:P1 10}}]

      (is (thrown+? [:type ::t/NOT-IN-SYSTEM]
                    (-> world (t/fire :P1)))))))

(deftest move-journal
  (testing "forward OK, one player"
    (let [arena (-> {}
                    (pl/add-player :P (pl/gen-player [0 0 0])))
          world {:arena arena :scoring nil}]
      (is (= [{:to :P :action :view :args {:x0 {:y0 :wall :y1 :wall :y2 :wall}
                                           :x1 {:y0 {:player :P} :y1 :empty :y2 :wall}
                                           :x2 {:y0 :empty :y1 :empty :y2 :wall}
                                           :manoeuvre :forward}}]
             (-> world
                 (t/move :P :forward)
                 (:journal))))))

  (testing "forward OK, two players"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [1 0 0])))
          world {:arena arena :scoring nil}]
      ;; The order of these journal items is implementation-dependent (we reduce over
      ;; the set of active players). TODO: we could sort them first.
      (is (= [{:to :P1 :action :view :args {:x0 {:y0 :wall :y1 :wall :y2 :wall}
                                            :x1 {:y0 {:player :P1} :y1 :empty :y2 :wall}
                                            :x2 {:y0 :empty :y1 :empty :y2 :wall}
                                            :manoeuvre :forward}}
              ;; No manoeuvre for P2.
              {:to :P2 :action :view :args {:x0 {:y0 :empty :y1 {:player :P1} :y2 :empty}
                                            :x1 {:y0 {:player :P2} :y1 :empty :y2 :empty}
                                            :x2 {:y0 :empty :y1 :empty :y2 :empty}}}]
             (-> world
                 (t/move :P1 :forward)
                 (:journal))))))

  (testing "forward blocked"
    (let [arena (-> {}
                    (pl/add-player :P (pl/gen-player [0 2 0])))
          world {:arena arena :scoring nil}]
      (is (= [{:to :P :action :blocked}]
             (-> world
                 (t/move :P :forward)
                 (:journal))))))

  (testing "yaw left"
    (let [arena (-> {}
                    (pl/add-player :P (pl/gen-player [0 0 0])))
          world {:arena arena :scoring nil}]
      (is (= [{:to :P :action :view :args {:x0 {:y0 :wall :y1 :wall :y2 :wall }
                                           :x1 {:y0 {:player :P} :y1 :wall :y2 :wall}
                                           :x2 {:y0 :empty :y1 :wall :y2 :wall}
                                           :manoeuvre :yaw-left}}]
             (-> world
                 (t/move :P :yaw-left)
                 (:journal)))))))

(deftest basic-round-scoring
  (testing "in scoring but not in round"
    ;; We can't really get an empty cube, but:
    (let [world {:arena {} :scoring {:P 10}}
          {j :journal} (t/fire world :P)]
      (is (= [{:to :P :action :error :args {:message "not currently in play"}}]
             j))))

  (testing "score hit"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))
          world0 {:arena arena :scoring {:P1 10 :P2 10}}
          world1 (t/fire world0 :P1)]
      (is (= [{:to :P1 :action :hit :args {:player :P2}}
              {:to :P2 :action :hit-by :args {:player :P1 :hit-points 9}}]
             (:journal world1)))
      (is (= 9 (:P2 (:scoring world1))))))

  (testing "knockout"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0]))
                    (pl/add-player :P3 (pl/gen-player [2 2 2])))
          world0 {:arena arena :scoring {:P1 1 :P2 1 :P3 10}}
          world1 (t/fire world0 :P1)]
      (is (= [{:to :P1 :action :hit :args {:player :P2}}
              {:to :P2 :action :hit-by :args {:player :P1 :hit-points 0}}
              {:to m/BROADCAST :action :dead :args {:player :P2}}]
             (:journal world1)))
      (is (= 0 (-> world1 (:scoring) (:P2))))
      (is (:P1 (:arena world1)))
      (is (nil? (:P2 (:arena world1))))
      (is (:P3 (:arena world1)))))

  (testing "knockout, end of round"
    (let [arena (-> {}
                    (pl/add-player :P1 (pl/gen-player [0 0 0]))
                    (pl/add-player :P2 (pl/gen-player [0 1 0])))
          world0 {:arena arena :scoring {:P1 1 :P2 1 :P3 10}}
          world1 (t/fire world0 :P1)]
      (is (= [{:to :P1 :action :hit :args {:player :P2}}
              {:to :P2 :action :hit-by :args {:player :P1 :hit-points 0}}
              {:to m/BROADCAST :action :dead :args {:player :P2}}
              {:to m/BROADCAST :action :end-round}
              {:to m/BROADCAST :action :alert :args {:message "round over, winner :P1"}}
              {:to m/BROADCAST :action :start-round}]
             (:journal world1)))
      ;; Players have been put back into play:
      (is (-> world1 (:arena) (:P1)))
      (is (-> world1 (:arena) (:P2)))
      (is (-> world1 (:arena) (:P3))))))
