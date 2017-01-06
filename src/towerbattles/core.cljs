(ns towerbattles.core
  (:require [reagent.core :as r]))

(enable-console-print!)

(defonce app-state (r/atom {:towers {}}))

(defn toggle-tower [towers y x]
  (update-in towers [y x] (fn [state]
                            (if (nil? state)
                              true))))

(defn game-board []
  (let [towers (:towers @app-state)]
    [:table.game-board
     [:tr
      {:key "start"}
      [:td.game-board__start {:col-span 8}]]
     (for [y (range 16)]
       [:tr
        {:key y}
        (for [x (range 8)]
          [:td.game-board__cell
           {:key x
            :class (str (if (get-in towers [y x])
                          "game-board__cell--tower"
                          "game-board__cell--empty"))
            :on-click #(swap! app-state update :towers toggle-tower y x)}
           ""])])
     [:tr
      {:key "end"}
      [:td.game-board__end {:col-span 8}]]]))

(defn main []
  [game-board])

(defn on-js-reload []
  (r/render [main] (js/document.getElementById "app")))

(on-js-reload)
