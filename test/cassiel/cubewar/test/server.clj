(ns cassiel.cubewar.test.server
  "(Non-network) server tests"
  (:use clojure.test)
  (:require (cassiel.cubewar [players :as pl]
                             [db :as db]
                             [server :as srv]))
  (:import [net.loadbang.osc.comms IPTransmitter]))

(def TEST-DB (db/mem-db "test"))

(use-fixtures :each (fn [t]
                      (db/initialize TEST-DB)
                      (t)))

(def state-n
  (-> {}
      (pl/add-player :P1 (pl/gen-player [0 0 0]))
      (pl/add-player :P2 (pl/gen-player [1 0 0]))
      (pl/add-player :P3 (pl/gen-player [0 1 0]))))

(def world-n {:arena state-n
              :scoring {:P1 50 :P3 50}
              :names->transmitters {}
              :origins->names {}
              :db TEST-DB})

(deftest housekeeping
  (testing "attach: world state"
    (let [WORLD (atom world-n)
          _ (srv/serve1 WORLD
                        nil
                        {:host "localhost" :port 9999}
                        :attach
                        [:P1 9998])]
      (is (= :P1 (-> @WORLD
                     (:origins->names)
                     (get {:host "localhost" :port 9999}))))
      (let [r (-> @WORLD
                  (:names->transmitters)
                  (:P1))]
        (is (isa? (class r) IPTransmitter))
        (is (= "localhost" (-> r (.getAddress) (.getHostName))))
        (is (= 9998 (.getPort r))))))

  (testing "attach: journal"
    (let [WORLD (atom world-n)]
      (is (= [{:to :P1 :action :welcome}
              {:to :P1 :action :attached :args {:host "localhost" :port 9998}}]
             (srv/serve1 WORLD
                         nil
                         {:host "localhost" :port 9999}
                         :attach
                         [:P1 9998]))))))

(deftest authentication
  (testing "bad command"
    (let [WORLD (atom world-n)
          handler (fn [world exn origin player-opt args]
                    {:journal (.getMessage exn)})
          do-it (srv/serve1 WORLD
                            handler
                            {:host "localhost" :port 9999}
                            :SOME-BAD-COMMAND
                            [9998])]
      (is (= "throw+: {:type :cassiel.cubewar.tournament/BAD-ACTION, :action :SOME-BAD-COMMAND}"
             do-it))))

  (testing "login as demo"
    (let [WORLD (atom world-n)
          handler (fn [world exn origin player-opt args] {:journal "FAILED"})
          _ (srv/serve1 WORLD
                        handler
                        {:host "localhost" :port 9999}
                        :login
                        ["Demo1" "Pass1" 9998])]
      (is (= "Demo1" (-> @WORLD
                         (:origins->names)
                         (get {:host "localhost" :port 9999}))))
      (let [r (-> @WORLD
                  (:names->transmitters)
                  (get "Demo1"))]
        (is (isa? (class r) IPTransmitter))
        (is (= "localhost" (-> r (.getAddress) (.getHostName))))
        (is (= 9998 (.getPort r))))))

  (testing "already logged in"
    (let [WORLD (atom world-n)
          handler (fn [world exn origin player-opt args] {:journal (.getMessage exn)})
          try-login #(srv/serve1 WORLD
                                 handler
                                 {:host "localhost" :port 9999}
                                 :login
                                 ["Demo1" "Pass1" 9998])
          _ (try-login)]
      (is (= "throw+: {:type :cassiel.cubewar.server/ALREADY-LOGGED-IN}" (try-login)))))

  (testing "repeated login OK"
    (let [WORLD (atom world-n)
          handler (fn [world exn origin player-opt args] {:journal "FAILED"})
          try-login #(srv/serve1 WORLD
                                 handler
                                 {:host "localhost" :port 9999}
                                 :login
                                 ["Demo1" "Pass1" 9998])
          try-logout #(srv/serve1 WORLD
                                  handler
                                  {:host "localhost" :port 9999}
                                  :detach
                                  [])
          _ (dorun [(try-login) (try-logout) (try-login)])]
      (is ((:scoring @WORLD) "Demo1"))))

  (testing "login failed"
    (let [WORLD (atom world-n)
          handler (fn [world exn origin player-opt args]
                    {:journal (.getMessage exn)})
          do-it (srv/serve1 WORLD
                            handler
                            {:host "localhost" :port 9999}
                            :login
                            ["WrongUser" "WrongPass" 9998])]
      (is (= "throw+: {:type :cassiel.cubewar.db/AUTH-FAILED}"
             do-it))))

  (testing "new user"
    (let [WORLD (atom world-n)
          handler (fn [world exn origin player-opt args] {:journal (.getMessage exn)})
          _ (srv/serve1 WORLD
                        handler
                        {:host "localhost" :port 9999}
                        :login-new
                        ["NewUser" "NewPass" 0xFFFFFF 9998])]
      (is (= "NewUser" (-> @WORLD
                         (:origins->names)
                         (get {:host "localhost" :port 9999}))))
      (let [r (-> @WORLD
                  (:names->transmitters)
                  (get "NewUser"))]
        (is (isa? (class r) IPTransmitter))
        (is (= "localhost" (-> r (.getAddress) (.getHostName))))
        (is (= 9998 (.getPort r))))))

  (testing "user exists"
    (let [WORLD (atom world-n)
          handler (fn [world exn origin player-opt args]
                    {:journal (.getMessage exn)})
          do-it (srv/serve1 WORLD
                            handler
                            {:host "localhost" :port 9999}
                            :login-new
                            ["Demo1" "OtherPass" 0xFFFFFF 9998])]
      (is (= "throw+: {:type :cassiel.cubewar.db/DUPLICATE-USER}"
             do-it)))))
