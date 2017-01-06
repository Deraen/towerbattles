(ns towerbattles.core
  (:require [reagent.core :as r]
            [towerbattles.astar :as astar]))

(enable-console-print!)

(defn distance-sq [[y1 x1] [y2 x2]]
  (+ (Math/pow (Math/abs (- y2 y1)) 2)
     (Math/pow (Math/abs (- x2 x1)) 2)))

(defn distance [start end]
  (Math/sqrt (distance-sq start end)))

(defn get-neighbors [towers [y x]]
  (concat [[-1 0] [1 0] [0 -1] [0 1]]
          #_
          (remove (fn [[dy dx]]
                    (and (get-in towers [(+ y dy) x])
                         (get-in towers [y (+ x dx)])))
                  [[-1 -1] [-1 1] [1 1] [1 -1]])))

(defn out? [[y x]]
  (not (and (<= 0 x 8)
            (<= -1 y 17))))

(defn move [[y x] [dy dx]]
  [(+ y dy) (+ x dx)])

(defn refresh-route [{:keys [towers] :as app-state}]
  (assoc app-state :route (time (astar/astar {:heurestic distance
                                              :distance distance
                                              :get-neighbors (fn [current closed?]
                                                               (->> (get-neighbors towers current)
                                                                    (map (partial move current))
                                                                    (remove closed?)
                                                                    (remove #(get-in towers %))
                                                                    (remove out?)))
                                              :start [-1 4]
                                              :goal [17 4]}))))

(def empty-state (refresh-route {:towers {}}))

(defonce app-state (r/atom empty-state))

(defn reset-game []
  (reset! app-state empty-state))

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

(defn tools []
  [:div.tools
   [:button.button
    {:type "button"
     :on-click #(reset-game)}
    "Reset"]

   [:ul
    [:li [:strong "Path length: "] (count (:route @app-state))]]])

(defn menu []
  [:div.menu
   [tools]])

(defn main []
  [:div.game-view
   [:div.board-container [game-board]]
   [menu]])

(defn on-js-reload []
  (r/render [main] (js/document.getElementById "app")))

(on-js-reload)
