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
                            :y (+ 100 (* 50 index))
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
    [:circle {:id (name language-key) :class (name language-key) :r radius}]])

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
        css-classes (str (name language1-key) " "
                         (name language2-key))]
    [:g
      (make-line :x1 dx :y1 dy
                 :x2 (- (* r1 (math/cos line-angle)))
                 :y2 (- (* r1 (math/sin line-angle)))
                 :class css-classes
                 :transform (translate :x x1 :y y1))]))

(defn make-language-connections [[index [language-key {{:keys [x y]} :svg influenced-by :influenced-by :as language}]]]
  [:g
    (map #(connect-languages [language-key language] [% (% preprocessed-languages)]) influenced-by)])

(defn make-all-language-connections [langs]
  (bind! "#connections"
         (unify (map-indexed vector (vec @langs)) make-language-connections)))

(defn make-all-language-nodes [langs]
  (bind! "#nodes"
         (unify (map-indexed vector (vec @langs)) make-language-node)))

(defn make-all-language-lables [langs]
  (bind! "#lables"
         (unify (map-indexed vector (vec @langs)) make-language-label)))

(defn class-pattern [class-name]
  (re-pattern (str "(^| )" (name class-name) "($| )")))

(defn has-class? [node class]
  (not (nil? (re-find (class-pattern class) (dom/attr node :class)))))

(defn add-class! [node class]
  (when-not (has-class? node class)
    (dom/attr node :class (s/trim (str (dom/attr node :class) " " (name class))))))

(defn remove-class! [node class]
  (when (has-class? node class)
    (dom/attr node :class (s/replace (dom/attr node :class) (class-pattern class) ""))))

(defn toggle-class! [node class]
  (if (has-class? node class)
    (remove-class! node class)
    (add-class! node class)))

(defn remove-active! []
  (doseq [node (dom/select-all ".active")]
    (remove-class! node :active)))

(defn scroll-centered! [x y]
  (let [screen (.-screen js/window)
        w (.-availWidth screen)
        h (.-availHeight screen)]
    (js/scrollTo (- x (/ w 2)) (- y (/ h 2)))))

(defn activate-language! [lang-key]
  (doseq [node (dom/select-all (str "." (name lang-key)))]
    (add-class! node :active)))

(defn on-node-click [[index [lang-key {{:keys [x y]} :svg}]] node event]
  (remove-active!)
  (scroll-centered! x y)
  (activate-language! lang-key))

(defn on-connection-click [data node event]
  (remove-active!)
  (let [line-node (.-target event)]
    (toggle-class! line-node :active)
    (doseq [language (remove (partial = "active") (s/split (dom/attr line-node :class) #" "))]
      (toggle-class! (dom/select (str "#" language)) :active))))

(defn make-graph! [langs]
  (make-all-language-connections langs)
  (make-all-language-nodes langs)
  (make-all-language-lables langs)
  (on "#lables" :click on-node-click)
  (on "#nodes" :click on-node-click)
  (on "#connections" :click on-connection-click))

(defn lower-case [s]
  (.toLowerCase s))

(defn string-contains? [string s]
  (not= -1 (.indexOf string s)))

(defn contains-lang-name? [lang lang-name]
  (string-contains? (-> lang :name lower-case) lang-name))

(defn find-langs-with-name [langs lang-name]
  (filter #(contains-lang-name? (val %) lang-name) langs))

(def shown-languages (atom preprocessed-languages))

(defn on-search-input [event]
  (swap! shown-languages (fn [cur-langs] (find-langs-with-name preprocessed-languages (-> event .-target .-value))))
  (let [first-lang (first @shown-languages)
        x (-> first-lang val :svg :x)
        y (-> first-lang val :svg :y)]
    (scroll-centered! x y)
    (activate-language! (key first-lang))))

(defn on-node-input [node f]
  (set! (.-oninput node) f))

(defn setup-search-handlers! []
  (on-node-input (dom/select "#search") on-search-input))

(defn on-window-load [f]
  (set! (.-onload js/window) f))

(on-window-load #(do (make-graph! shown-languages)
                     (setup-search-handlers!)))

