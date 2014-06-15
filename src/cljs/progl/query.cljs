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

(def langs {:ksh {:influenced #{:windows-powershell :zsh}, :appearance-year 1984, :name "Korn Shell", :creators #{"David Korn"}, :influenced-by #{:sh}, :url "/wiki/Korn_shell"},
 :lua {:influenced #{:falcon :squirrel}, :appearance-year 1993, :name "Lua", :creators #{"PUC-Rio" "Roberto Ierusalimschy et al. at Tecgraf"}, :influenced-by #{:clu :snobol :cplusplus :modula :scheme}, :url "/wiki/Lua_(programming_language)"}})



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

(def subquery-separator ",")

(defn subqueries [lang-query]
  (remove empty? (map s/trim (s/split lang-query (re-pattern subquery-separator)))))

(defn query [langs lang-query]
  (if (= "" lang-query) {}
    (if-not (string-contains? lang-query subquery-separator)
      (let [filter-pred? (lang-filter-pred lang-query)]
        (apply hash-map (flatten (sort-by #(-> % val :name) (filter filter-pred? langs)))))
      (apply merge (map #(query langs %) (subqueries lang-query))))))
