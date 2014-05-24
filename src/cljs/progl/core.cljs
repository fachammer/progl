(ns progl.core
  (:use-macros [c2.util :only [bind!]])
  (:require [progl.languages :as l]
            [c2.scale :as scale]
            [c2.dom :as dom]
            [c2.maths :as math]
            [clojure.string :as s])
  (:use [c2.core :only [unify]]
        [c2.event :only [on]]))

(defn year-scale [value]
  (int ((scale/linear :domain [1942 2014]
                :range [0 100]) value)))

(defn pixel-scale [percentage dimension]
  (int ((scale/linear :domain [0 100]
                :range [0 dimension]) percentage)))

(defn percentage [value]
  (str percentage "%"))

(defn pixels [value]
  (str value "px"))

(defn translate [& {:keys [x y]}]
  (str "translate(" x " " y ")"))

(defn scale [scale-factor]
  (str "scale(" scale-factor ")"))

(defn calculate-year-intervals [min-year max-year interval-size]
  (map #(apply hash-set %) (partition-all interval-size (range min-year max-year))))

(def year-intervals (calculate-year-intervals 1943 2014 4))

(defn preprocess-language [[index [language-key {:keys [appearance-year] :as language}]]]
  {language-key
    (let [window-width 12000]
      (assoc language :svg {:x (pixel-scale (year-scale appearance-year) window-width)
                            :y (+ 60 (* 50 index))
                            :radius 40}))})

(defn preprocess-language-year [language-year-group]
  (map preprocess-language (map-indexed vector language-year-group)))

(defn year-interval [year]
  (flatten (filter #(contains? % year) year-intervals)))

(defn influenced-languages [language languages]
  (apply hash-set (keys (filter #(contains? (:influenced-by (val %)) language) languages))))

(defn filter-languages [languages]
  (filter #(<= 1 (count (influenced-languages (key %) languages))) languages))

(defn preprocess-languages [languages]
  (reduce into (flatten (map preprocess-language-year (vals (group-by #(year-interval (:appearance-year (val %))) (filter-languages languages)))))))

(def preprocessed-languages (preprocess-languages l/languages))

(defn make-language-node [[index [language-key {{:keys [x y radius]} :svg}]]]
  [:g {:transform  (translate :x x :y y)}
    [:circle {:id (name language-key) :r radius}]])

(defn overflow-text [text]
  (if (>= (count text) 11)
    (str (subs text 0 11) "...")
    text))

(defn make-language-label [[index [language-key {{:keys [x y]} :svg name :name appearance-year :appearance-year}]]]
  [:g {:transform  (translate :x x :y y)}
    [:text {:dx 0 :dy 0} (overflow-text name)]])

(defn make-line [& {:keys [x1 y1 x2 y2 style] :as line}]
  [:line (assoc line :style (into {:marker-end "url(#markerArrow)"} style))])

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
        css-classes (str (name language1-key) " "
                         (name language2-key))]
    [:g
      (make-line :x1 dx :y1 dy
                 :x2 (- (* r1 (math/cos line-angle)))
                 :y2 (- (* r1 (math/sin line-angle)))
                 :class css-classes
                 :transform (translate :x x1 :y y1))]))

(defn make-language-connections [[index [language-key {{:keys [x y]} :svg influenced-by :influenced-by :as language}]]]
  (map #(connect-languages [language-key language] [% (% preprocessed-languages)]) influenced-by))

(defn make-all-language-connections []
  (bind! "#connections"
         (unify (map-indexed vector (vec (preprocess-languages l/languages))) make-language-connections)))

(defn make-all-language-nodes []
  (bind! "#nodes"
         (unify (map-indexed vector (vec (preprocess-languages l/languages))) make-language-node)))

(defn make-all-language-lables []
  (bind! "#lables"
         (unify (map-indexed vector (vec (preprocess-languages l/languages))) make-language-label)))

(defn add-class! [node class]
  (dom/attr node :class (str (dom/attr node :class) " " (name class))))

(defn remove-class! [node class]
  (dom/attr node :class (s/replace (dom/attr node :class) (re-pattern (str "(^| )" (name class) "($| )")) "")))

(defn has-class? [node class]
  (not (nil? (re-find (re-pattern (str "(^| )" (name class) "($| )")) (dom/attr node :class)))))

(defn toggle-class! [node class]
  (if (has-class? node class)
    (remove-class! node class)
    (add-class! node class)))

(defn on-node-click [[index [language-key language]] node event]
  (toggle-class! node :active)
  (doseq [node (dom/select-all (str "." (name language-key)))]
    (toggle-class! node :active)))

(defn on-connection-click [data node event]
  (toggle-class! node :active)
  (.log js/console node)
  (let [line-node ((dom/children ((dom/children node) 0)) 0)]
    (doseq [language (s/split (dom/attr line-node :class) #" ")]
      (toggle-class! (dom/select (str "#" language)) :active))))

(defn make-graph! []
  (make-all-language-connections)
  (make-all-language-nodes)
  (make-all-language-lables)
  (on "#lables" :click on-node-click)
  (on "#nodes" :click on-node-click)
  (on "#connections" :click on-connection-click))

(defn on-window-load [f]
  (set! (.-onload js/window) f))

(on-window-load make-graph!)

