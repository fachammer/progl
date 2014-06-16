(ns progl.query
  (:require [clojure.string :as s])
  (:use [clojure.set :only [intersection]]))

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

(declare single-query)

(def union-query-separator ",")
(declare union-query)

(def intersection-query-separator " ")
(declare intersection-query)

(defn query [langs lang-query]
  (if (= "" lang-query)
    {}
    (cond
     (and (string-starts-with? lang-query "\"") (string-ends-with? lang-query "\"") (not (string-contains? lang-query union-query-separator))) (single-query langs lang-query)
     (string-contains? lang-query union-query-separator) (union-query langs lang-query)
     (string-contains? lang-query intersection-query-separator) (intersection-query langs lang-query)
     :else (single-query langs lang-query))))

(defn equal-name-pred [lang-query]
  (fn [lang#]
    (let [query (-> lang-query (s/replace "\"" "") s/trim lower-case)]
      (or
        (= query (-> lang# val :name lower-case))
        (= query (-> lang# val :appearance-year str))))))

(defn contains-name-pred [lang-query]
  (fn [lang#]
    (let [query (-> lang-query s/trim lower-case)]
      (or
       (string-contains? (-> lang# val :name lower-case)
                         lang-query)
       (string-contains? (-> lang# val :appearance-year str)
                         lang-query)
       (some #(string-contains? (lower-case %) query) (-> lang# val :creators))))))

(defn lang-filter-pred [lang-query]
  (if (and (string-starts-with? lang-query "\"") (string-ends-with? lang-query "\""))
    (equal-name-pred lang-query)
    (contains-name-pred lang-query)))

(defn single-query [langs query]
  (let [filter-pred? (lang-filter-pred query)]
    (->> (filter filter-pred? langs)
         (sort-by #(-> % val :name))
         flatten
         (apply hash-map))))

(defn union-subqueries [lang-query]
  (->> (re-pattern union-query-separator)
       (s/split lang-query)
       (map s/trim)
       (remove empty?)))

(defn union-query [langs lang-query]
  (->> (union-subqueries lang-query)
       (map #(query langs %))
       (apply merge)))

(defn intersection-subqueries [query]
  (->> (re-pattern intersection-query-separator)
       (s/split query)
       (map #(s/replace % #"\"" ""))
       (map s/trim)
       (remove empty?)))

(defn intersection-query [langs lang-query]
  (->> (intersection-subqueries lang-query)
       (map #(->> (query langs %) keys (apply hash-set)))
       (apply clojure.set/intersection)
       (map (fn [k] [k (k langs)]))
       flatten
       (apply hash-map)))
