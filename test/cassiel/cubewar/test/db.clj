(ns cassiel.cubewar.test.db
  "Database testing."
  (:use clojure.test
        slingshot.test)
  (:require (cassiel.cubewar [db :as db])))

(def TEST-DB (db/mem-db "test"))

(use-fixtures :each (fn [t]
                      (db/clear TEST-DB)
                      (t)))

(deftest basics
  (testing "initial DB state"
    (is (= 0 (db/num-users TEST-DB)))))
