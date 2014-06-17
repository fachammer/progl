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

(declare query)

(def influences-operator ">")
(def influenced-operator "<")

(defn influences-query [langs lang-query]
  (->> (-> lang-query
           (s/replace (re-pattern influences-operator) "")
           (s/replace (re-pattern influenced-operator) ""))
       (query langs)
       (map #(-> % val :influenced-by))
       (reduce into)
       (map (fn [k] [k (k langs)]))
       flatten
       (apply hash-map)))

(defn influenced-query [langs lang-query]
  (->> (-> lang-query
           (s/replace (re-pattern influences-operator) "")
           (s/replace (re-pattern influenced-operator) ""))
       (query langs)
       (map #(-> % val :influenced))
       (reduce into)
       (map (fn [k] [k (k langs)]))
       flatten
       (apply hash-map)))

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

(defn single-query [langs lang-query]
  (cond
   (string-starts-with? lang-query influences-operator)
     (let [influences (influences-query langs lang-query)]
        (if (string-ends-with? lang-query influenced-operator)
          (merge influences (influenced-query langs lang-query))
          influences))
   (string-ends-with? lang-query influenced-operator)
     (influenced-query langs lang-query)
   :else
     (let [filter-pred? (lang-filter-pred lang-query)]
      (->> (filter filter-pred? langs)
           (sort-by #(-> % val :name))
           flatten
           (apply hash-map)))))

(def union-query-separator ",")

(defn union-subqueries [lang-query]
  (->> (re-seq (re-pattern (str "[^" union-query-separator "\"]+|\"[^\"]*\"")) lang-query)
       (map #(s/replace % #"\"" ""))
       (map s/trim)
       (remove empty?)))

(defn union-query [langs lang-query]
  (->> (union-subqueries lang-query)
       (map #(query langs %))
       (apply merge)))

(def intersection-query-separator " ")

(defn intersection-subqueries [lang-query]
  (->> (re-seq (re-pattern (str "[^" intersection-query-separator "\"]+|\"[^\"]*\"")) lang-query)
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

(defn query [langs lang-query]
  (if (= "" lang-query)
    {}
    (if-let [union-queries (union-subqueries lang-query)]
      (union-query langs lang-query)
      (if-let [intersection-queries (intersection-subqueries lang-query)]
        (intersection-query langs lang-query)
        (single-query langs lang-query)))))

