(ns towerbattles.core
  (:require [reagent.core :as r]))

(enable-console-print!)

(defonce app-state (r/atom {:towers {}}))

(defn distance-sq [[y1 x1] [y2 x2]]
  (+ (Math/pow (Math/abs (- y2 y1)) 2)
     (Math/pow (Math/abs (- x2 x1)) 2)))

(defn distance [start end]
  (Math/sqrt (distance-sq start end)))

(defn construct-path [came-from current]
  (loop [current current
         path [current]]
    (if-let [next (get came-from current)]
      (recur next (conj path next))
      path)))

(defn astar [towers start end]
  (let [came-from (atom {})]
    (loop [
           open? #{start}
           closed? #{}
           g-score {start 0}
           f-score {start (distance start end)}]
      (if (not (empty? open?))
        (let [[[y x :as current]] (reduce (fn [[current score] this]
                                            (let [this-score (get f-score this)]
                                              (if (or (nil? current)
                                                      (< this-score score))
                                                [this this-score]
                                                [current score])))
                                          [nil nil]
                                          open?)]
          (if (= current end)
            (construct-path @came-from current)

            (let [closed? (conj closed? current)
                  [open? g-score f-score _]
                  (loop [open? (disj open? current)
                         g-score g-score
                         f-score f-score
                         neighbors (->> [[-1 0] [1 0] [0 -1] [0 1]]
                                        (map (fn [[my mx]]
                                               [(+ y my) (+ x mx)]))
                                        (remove closed?)
                                        (remove (fn [[y x]]
                                                  (get-in towers [y x])))
                                        (remove (fn [[y x]]
                                                  (not (and (<= 0 x 8)
                                                            (<= -1 y 17))))))]
                    (if (empty? neighbors)
                      [open? g-score f-score]
                      (let [neighbor (first neighbors)
                            t-score (+ (get g-score current) (distance current neighbor))]
                        (if (<= t-score (get g-score neighbor))
                          (recur (conj open? neighbor)
                                 g-score
                                 f-score
                                 (rest neighbors))
                          (do
                            (swap! came-from assoc neighbor current)
                            (recur (conj open? neighbor)
                                   (assoc g-score neighbor t-score)
                                   (assoc f-score neighbor (+ t-score (distance neighbor end)))
                                   (rest neighbors)))))))]
              (recur open? closed? g-score f-score))))
        nil))))

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
