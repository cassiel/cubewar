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

    (let [msg (net/make-message :foo {:bar 67 :X "baz"})]
      (is (= "/foo" (.getAddress msg)))
      (is (= 6 (.getNumArguments msg)))
      (is (= ["bar" ":" 67 "X" ":" "baz"] (map #(.getValue (.getArgument msg %)) (range 6))))))

  (testing "dictionary formatting"
    (letfn [(decomp [msg] (map #(.getValue (.getArgument msg %))
                               (range (.getNumArguments msg))))]
      (is (= ["A" ":" 3 "B" ":" 5]
             (decomp (net/make-message :BOGUS {:A 3 :B 5}))))
      (is (= ["A" ":" "{" "B" ":" 4 "}" "C" ":" 6 7 8]
             (decomp (net/make-message :BOGUS {:A {:B 4} :C [6 7 8]}))))
      (is (= ["A" ":" "{" "}"]
             (decomp (net/make-message :BOGUS {:A {}})))))))
