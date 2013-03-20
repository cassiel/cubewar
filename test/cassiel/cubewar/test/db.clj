(ns cassiel.cubewar.test.db
  "Database testing."
  (:use clojure.test
        midje.sweet
        slingshot.test)
  (:require (cassiel.cubewar [db :as db])))

(def TEST-DB (db/mem-db "test"))
;(def TEST-DB (db/file-db "test_hsqldb"))

(use-fixtures :each (fn [t]
                      (db/clear TEST-DB)
                      (t)))

(deftest basics
  (testing "initial DB state"
    (is (= 0 (db/num-users TEST-DB)))))

(deftest user-management
  (testing "add"
    (let [_ (db/add-user TEST-DB "User" "Pass" 0)]
      (is (nil? (db/lookup-id TEST-DB "Bogus")))
      (is (= 1 (db/num-users TEST-DB)))
      (is (thrown+? [:type ::db/DUPLICATE-USER]
                    (db/add-user TEST-DB "User" "Pass2" 0))))))

(deftest basic-authentication
  (testing "auth tests"
    (let [_ (db/add-user TEST-DB "User" "Pass" 0)]
      (is (db/authenticate TEST-DB "User" "Pass"))

      (is (thrown+? [:type ::db/AUTH-FAILED]
                    (db/authenticate TEST-DB "User" "Wrong-Pass")))
      (is (thrown+? [:type ::db/AUTH-FAILED]
                    (db/authenticate TEST-DB "Bogus" "Fogus"))))))


(with-state-changes [(before :facts (db/clear TEST-DB))]
  (facts "scoring"
         (fact "winner"
               (let [_1 (db/add-user TEST-DB "User1" "Pass1" 0)
                     _2 (db/add-user TEST-DB "User2" "Pass2" 0)
                     _3 (db/add-user TEST-DB "User3" "Pass3" 0)
                     _ (dorun [(db/out-of-round TEST-DB "User1")
                               (db/out-of-round TEST-DB "User2")
                               (db/winner TEST-DB "User1")])]
                 (db/score TEST-DB "User1") => {:played 1 :won 1}
                 (db/score TEST-DB "User2") => {:played 1 :won 0}
                 (db/score TEST-DB "User3") => {:played 0 :won 0}
                 (db/league TEST-DB) => [{:name "User1" :played 1 :won 1}
                                         {:name "User2" :played 1 :won 0}
                                         {:name "User3" :played 0 :won 0}]))))
