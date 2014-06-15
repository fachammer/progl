(ns progl.ui.graph
  (:require-macros [c2.util :refer [bind!]]
                   [cljs.core.async.macros :as m :refer [go]])
  (:require [progl.languages :as l]
            [progl.ui.select :as select]
            [progl.ui.highlighting :as h]
            [progl.dom :as dom]
            [progl.util :refer [with-quotes on-channel]]
            [c2.scale :as scale]
            [c2.maths :as math]
            [c2.core :refer [unify]]
            [c2.event :refer [on]]
            [c2.dom :refer [attr]]
            [clojure.string :as s]
            [cljs.core.async :as async :refer [>! pipe map<]]))

(defn year-scale [value]
  ((scale/linear :domain [1942 2014]
                :range [0 100]) value))

(defn pixel-scale [percentage dimension]
  (int ((scale/linear :domain [0 100]
                :range [0 dimension]) percentage)))

(defn translate [& {:keys [x y]}]
  (str "translate(" x " " y ")"))

(defn calculate-year-intervals [min-year max-year interval-size]
  (map #(apply hash-set %) (partition-all interval-size (range min-year max-year))))

(def year-intervals (calculate-year-intervals 1943 2014 4))

(defn preprocess-language [[index [language-key {:keys [appearance-year] :as language}]]]
  {language-key
    (let [window-width 12000]
      (assoc language :svg {:x (pixel-scale (year-scale appearance-year) window-width)
                            :y (+ 100 (* 300 index))
                            :radius 50}))})

(defn preprocess-language-year [language-year-group]
  (map preprocess-language (map-indexed vector language-year-group)))

(defn year-interval [year]
  (flatten (filter #(contains? % year) year-intervals)))

(defn filter-languages [languages]
  languages)

(defn preprocess-languages [languages]
  (apply merge (flatten (map preprocess-language-year (vals (group-by #(year-interval (:appearance-year (val %))) (filter-languages languages)))))))

(defn make-language-node [[index [language-key {{:keys [x y radius]} :svg}]]]
  [:g {:transform  (translate :x x :y y)}
    [:circle {:class (str "node " (name language-key))
              :r radius}]])

(defn overflow-text [text]
  (if (>= (count text) 11)
    (str (subs text 0 11) "...")
    text))

(defn make-language-label [[index [language-key {{:keys [x y]} :svg language-name :name appearance-year :appearance-year}]]]
  [:g {:transform  (translate :x x :y y)}
    [:text {:dx 0 :dy 0 :class (name language-key)} (overflow-text language-name)]])

(defn make-line [& {:keys [x1 y1 x2 y2 style] :as line}]
  [:line (assoc line :style style)])

(defn connect-languages [[language1-key language1] [language2-key language2]]
  (let [x1 (:x (:svg language1))
        y1 (:y (:svg language1))
        x2 (:x (:svg language2))
        y2 (:y (:svg language2))
        r1 (:radius (:svg language1))
        r2 (:radius (:svg language2))
        dx (- x2 x1)
        dy (- y2 y1)
        line-angle (math/atan (/ (- y1 y2) (- x1 x2)))
        css-classes (str "connection " (name language1-key) " " (name language2-key))]
    [:g
      (make-line :x1 dx :y1 dy
                 :x2 (- (* r1 (math/cos line-angle)))
                 :y2 (- (* r1 (math/sin line-angle)))
                 :class css-classes
                 :transform (translate :x x1 :y y1))]))

(defn make-language-connections [[index [language-key {{:keys [x y]} :svg influenced-by :influenced-by :as language}] languages]]
  [:g
    (map #(connect-languages [language-key language] [% (% languages)]) (filter (partial contains? languages) influenced-by))])

(defn year-grid-element [year]
  [:g {:transform (translate :x (pixel-scale (year-scale year) 12000) :y 20)}
   [:text {:class "year" :dx 0 :dy -10} year]
   (make-line :x1 0
              :x2 0
              :y1 0
              :y2 9000
              :class "year")])

(defn year-grid [id]
  (bind! id (unify (range 1943 2015) year-grid-element)))

(defn make-all-language-connections [langs]
  (bind! "#connections"
         (unify (map-indexed #(vector %1 %2 langs) (vec langs)) make-language-connections)))

(defn lang-node [lang-key]
  (dom/element-by-class js/document :node lang-key))

(defn lang-connections [lang-key]
  (dom/elements-by-class js/document :connection lang-key))

(defn apply-to-nodes [lang-keys f & args]
  (doseq [lk lang-keys]
    (apply f (lang-node lk) args)
    (doseq [conn (lang-connections lk)]
      (apply f conn args))))

(defn select-nodes [langs]
  (dom/remove-classes! :selected)
  (dom/remove-classes! :highlight)
  (dom/remove-classes! :sec-highlight)
  (apply-to-nodes (keys langs) dom/add-class! :selected))

(defn highlight-nodes [[prim-lang & sec-langs]]
  (apply-to-nodes [prim-lang] dom/add-class! :highlight)
  (when sec-langs
    (apply-to-nodes sec-langs dom/add-class! :sec-highlight)))

(defn dehighlight-nodes [lang-keys]
  (apply-to-nodes lang-keys dom/remove-class! :highlight)
  (apply-to-nodes lang-keys dom/remove-class! :sec-highlight))

(defn node-click->query [[[_ [_ {lang-name :name}]] _ _]] (with-quotes lang-name))

(defn mouseover->lang-key [[[_ [langk _]] _ _]] [langk])

(defn make-all-language-nodes [langs]
  (bind! "#nodes"
         (unify (map-indexed vector (vec langs)) make-language-node))
  (pipe (map< node-click->query (dom/c2-listen "#nodes" :click)) select/in)
  (pipe (map< mouseover->lang-key (dom/c2-listen "#nodes" :mouseover)) h/highlight-in)
  (pipe (map< mouseover->lang-key (dom/c2-listen "#nodes" :mouseout)) h/dehighlight-in)
  (on-channel select/out select-nodes)
  (on-channel h/highlight-out highlight-nodes)
  (on-channel h/dehighlight-out dehighlight-nodes))

(defn make-all-language-lables [langs]
  (bind! "#lables"
         (unify (map-indexed vector (vec langs)) make-language-label)))

(defn language-graph [graph-id langs]
  (let [preproc-langs (preprocess-languages langs)]
    ;(year-grid "#year-grid")
    (make-all-language-connections preproc-langs)
    (make-all-language-nodes preproc-langs)
    ;(make-all-language-lables preproc-langs)
    ))
