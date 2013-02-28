(ns cassiel.cubewar.test.network
  "Basic network machinery tests."
  (:use clojure.test)
  (:require (cassiel.cubewar [network :as net]))
  (:import [net.loadbang.osc.data Message]))

(deftest data
  (testing "message formatting"
    (is (= [:A [34 4.5 "Hello"]]
           (net/dispatch-message (fn [_ a args] [a args])
                                 nil
                                 (-> (Message. "/A")
                                     (.addInteger 34)
                                     (.addFloat 4.5)
                                     (.addString "Hello")))))

    (let [msg (net/make-message :foo [:bar 67 "baz"])]
      (is (= "/foo" (.getAddress msg)))
      (is (= 3 (.getNumArguments msg)))
      (is (= ["bar" 67 "baz"] (map #(.getValue (.getArgument msg %)) (range 3)))))))
