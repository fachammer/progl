(ns progl.query
  (:require [clojure.string :as s]))

(defn lower-case [s]
  (.toLowerCase s))

(defn string-contains? [string s]
  (not= -1 (.indexOf string s)))

(defn string-starts-with? [string s]
  (= 0 (.indexOf string s)))

(defn string-ends-with? [string s]
  (if (re-find (re-pattern (str s "$")) string)
    true
    false))

(defn lang-filter-pred [lang-name]
  (if (and (string-starts-with? lang-name "\"") (string-ends-with? lang-name "\""))
    =
    string-contains?))

(defn query [langs lang-query]
  (if (= "" lang-query)
    {}
    (let [filter-pred (lang-filter-pred lang-query)]
      (apply hash-map (flatten (filter #(filter-pred (lower-case (-> % val :name)) (-> lang-query (s/replace #"\"" "") lower-case)) langs))))))
