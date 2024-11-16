(ns clojure-demo.clojure-demo-test
  (:require
    [clojure.test :refer [deftest testing is]]))

;; TODO: test things

(deftest a-test
  (testing "Tautology."
    (is (= 1 1))))
