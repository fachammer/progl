(ns progl.language-scraper
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn fetch-actual-timeline []
  (fetch-url "http://en.wikipedia.org/wiki/Timeline_of_programming_languages"))

(def tl (fetch-actual-timeline))

(defn fetch-timeline [] tl)

(defn select-rows []
  (html/select (fetch-timeline) [:table.wikitable :tr]))

(defn select-years []
  (html/select (select-rows) [[:td (html/nth-child 1)]]))

(defn select-names []
  (html/select (select-rows) [[:td (html/nth-child 2)]]))

(defn select-creators []
  (html/select (select-rows) [[:td (html/nth-child 3)]]))

(defn select-predecessors []
  (html/select (select-rows) [[:td (html/nth-child 4)]]))

(defn scrape-records []
  (map (fn [year name creators predecessors]
         { :appearance-year year
           :name name
           :creators creators
           :influenced-by predecessors}) (html/texts (select-years))
                                         (html/texts (select-names))
                                         (html/texts (select-creators))
                                         (html/texts (select-predecessors))))

(defn column [col-key]
  (map col-key (scrape-records)))

(defn parse-years []
  (map #(read-string (subs % 0 4)) (column :appearance-year)))

(defn parse-names []
  (map #(s/replace % #" \(.*\).*$" "") (column :name)))

(defn parse-creators []
  (map #(apply hash-set (map s/trim (s/split % #","))) (column :creators)))

(def name->keyword-exceptions {"SQL aka structured query language" :sql
                               "GOM – Good Old Mad" :gom
                               "MAD – Michigan Algorithm Decoder" :mad
                               "Korn Shell" :ksh
                               "Bourne Shell" :sh
                               "Z Shell" :zsh
                               "JOSS" :joss-i
                               "Business BASIC" :basic
                               "ARC" :arc-assembly
                               "ALGOL" :algol-58
                               "Delphi" :object-pascal
                               "dBase" :vulcan-dbase-ii
                               "Licenced from Microsoft" :basic
                               "SORT/MERGE" :sort-merge-generator
                               "Ada" :ada-80
                               "QBasic" :basic
                               "Laning and Zierler" :laning-and-zierler-system
                               "IPL" :ipl-i
                               "Glennie Autocode" :autocode
                               "MetaHaskell" :haskell
                               "A-2" :a-0
                               "Unix shell" :sh
                               "BASIC/Z" :basic
                               "Smalltalk-72" :smalltalk
                               "Algol60" :algol-60
                               "ALGOL?" :algol-58
                               "Java?" :java
                               "Ruby?" :ruby
                               "Standard C" :c
                               "Smalltalk-80" :smalltalk
                               "Boehm" :boehm-unnamed-coding-system
                               "Turbo Pascal" :pascal})

(defn name->keyword [string]
  (keyword
   (if (contains? name->keyword-exceptions string)
     (name->keyword-exceptions string)
     (-> string (s/replace #"[()]" "") (s/replace #"#" "sharp")(s/replace #" " "-") (s/lower-case)))))

(defn language-keywords []
  (apply hash-set (map name->keyword (parse-names))))

(def language-keywords (language-keywords))

(defn language-exists? [language-key]
  (contains? language-keywords language-key))

(defn parse-predecessors []
  (map #(cond (or (= % "*") (= % "")) #{}
              :else (apply hash-set (filter language-exists? (map name->keyword (map s/trim (s/split % #","))))))
       (column :influenced-by)))

(defn make-map-entry [year name creators predecessors]
  {(name->keyword name){:appearance-year year
                        :name name
                        :creators creators
                        :influenced-by predecessors}})

(defn timeline []
  (reduce into (map make-map-entry (parse-years) (parse-names) (parse-creators) (parse-predecessors))))

(defn languages-with-nonexistent-predecessor []
  (let [tl (timeline)]
    (sort (filter (fn [timeline-entry] (some #(not (contains? tl %)) (:influenced-by (val timeline-entry)))) tl))))

(defn nonexistent-predecessors []
  (let [tl (timeline)]
    (filter #(not (contains? tl %)) (reduce into (map #(:influenced-by (val %)) tl)))))

(timeline)






