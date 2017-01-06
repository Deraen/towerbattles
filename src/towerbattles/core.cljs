(ns towerbattles.core
  (:require [reagent.core :as r]))

(enable-console-print!)

(defonce app-state (r/atom {:towers {}}))

(defn distance-sq [[y1 x1] [y2 x2]]
  (+ (Math/pow (Math/abs (- y2 y1)) 2)
     (Math/pow (Math/abs (- x2 x1)) 2)))

(defn distance [start end]
  (Math/sqrt (distance-sq start end)))

(defn- construct-path [came-from current]
  (loop [current current
         path [current]]
    (if-let [next (get came-from current)]
      (recur next (conj path next))
      path)))

(defn- get-best-node [f-score open?]
  (first (reduce (fn [[current score] this]
                   (let [this-score (get f-score this)]
                     (if (or (nil? current)
                             (< this-score score))
                       [this this-score]
                       [current score])))
                 [nil nil]
                 open?)))

(defn- get-neigbors [[y x] towers]
  (concat [[-1 0] [1 0] [0 -1] [0 1]]
          (remove (fn [[dy dx]]
                    (and (get-in towers [(+ y dy) x])
                         (get-in towers [y (+ x dx)])))
                  [[-1 -1] [-1 1] [1 1] [1 -1]])))

(defn out? [[y x]]
  (not (and (<= 0 x 8)
            (<= -1 y 17))))

(defn move [[y x] [dy dx]]
  [(+ y dy) (+ x dx)])

(defn astar [towers start end]
  (loop [open? #{start}
         closed? #{}
         g-score {start 0}
         f-score {start (distance start end)}
         came-from {}]
    (if (empty? open?)
      nil
      (let [current (get-best-node f-score open?)]
        (if (= current end)
          (construct-path came-from current)
          (let [closed? (conj closed? current)
                [open? g-score f-score came-from _]
                (loop [open? (disj open? current)
                       g-score g-score
                       f-score f-score
                       came-from came-from
                       neighbors (->> (get-neigbors current towers)
                                      (map (partial move current))
                                      (remove closed?)
                                      (remove #(get-in towers %))
                                      (remove out?))]
                  (if-let [neighbor (first neighbors)]
                    (let [t-score (+ (get g-score current) (distance current neighbor))]
                      (if (<= t-score (get g-score neighbor))
                        (recur (conj open? neighbor)
                               g-score
                               f-score
                               came-from
                               (rest neighbors))
                        (recur (conj open? neighbor)
                               (assoc g-score neighbor t-score)
                               (assoc f-score neighbor (+ t-score (distance neighbor end)))
                               (assoc came-from neighbor current)
                               (rest neighbors))))
                    [open? g-score f-score came-from]))]
            (recur open? closed? g-score f-score came-from)))))))

(defn refresh-route [app-state]
  (assoc app-state :route (time (astar (:towers app-state) [-1 4] [17 4]))))

(defn toggle-tower [app-state y x]
  (-> app-state
      (update-in [:towers y x] (fn [state]
                                 (if (nil? state)
                                   true)))
      refresh-route))

(defn game-board []
  (let [towers (:towers @app-state)
        route (set (:route @app-state))]
    [:table.game-board
     [:thead
      [:tr
       {:key "start"}
       [:td.game-board__start {:col-span 9}]]]
     [:tbody
      (for [y (range 16)]
        [:tr
         {:key y}
         (for [x (range 9)]
           [:td.game-board__cell
            {:key x
             :class (str (if (get-in towers [y x])
                           "game-board__cell--tower "
                           "game-board__cell--empty ")
                         (if (contains? route [y x])
                           "game-board__cell--route "))
             :on-click #(swap! app-state toggle-tower y x)}
            ""])])]
     [:tfoot
      [:tr
       {:key "end"}
       [:td.game-board__end {:col-span 9}]]]]))

(defn main []
  [game-board])

(defn on-js-reload []
  (r/render [main] (js/document.getElementById "app")))

(on-js-reload)
