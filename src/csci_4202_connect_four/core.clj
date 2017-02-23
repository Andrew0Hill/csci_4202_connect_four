(ns csci-4202-connect-four.core
  (:gen-class)
  (:require [cheshire.core :refer :all]
            [clojure.core.matrix :refer :all])
  (:import (java.io BufferedReader BufferedWriter)
           (java.lang.System)))

(def move-values {4 100000 3 10000 2 100 1 0})
(declare min-r)
(declare max-r)

(defn receive-game-string []
  (parse-string (read-line)))


(def MAXDEPTH 2)
(defn apply-move
  [state move player]
  ;; Bind column to the column selected by 'move'
  (let [column (get state move)]
    ;; If the first entry in the column is non-zero, then return nil because this column is full.
    (if-not (= (first column) 0)
      nil
      ;; If the last element in the column is zero, immediately insert at the end.
      (if (= (peek column) 0)
        ;; Associate the value at [move column.size-1] with the player's number (represents their move)
        (assoc-in state [move (- (count column) 1)] player)
        ;; Loop with initial index binding of 1 (We already checked zero in the first if-statement.
        (loop [index 1]
          ;; If this value is zero, recursively call with ++index.
          (if (= (get column index) 0)
            ;; If true recursively call.
            (recur (inc index))
            ;; If false, stop here and associate the given position with the player's number (move).
            (assoc-in state [move (- index 1)] player)

            ))))))

(defn get-valid-moves [state player]
  (into {}
        (map-indexed
          (fn [keyv val] [keyv val])
          (map #(apply-move state % player) (range (count state)))
          )
        )
  )
(defn switch-player [player]
  (if (= player 1)
    2
    1
    )
  )
(defn get-column-fours [state]
  (apply concat
         (map #(partition 4 1 %) state))
  )
(defn get-row-fours [state]
  (apply concat
         (map #(partition 4 1 %) (transpose state)))
  )
(defn get-diagonal-fours [state]
  (let [vec [] rev (reverse state)]
    (apply concat
           (map #(partition 4 1 %)
                (filter
                  #(>= (count %) 4)
                  (apply concat
                         (for [x (range (- 1 (count state)) (count (first state)))]
                           (conj vec (diagonal state x) (diagonal rev x))
                           )
                         )
                  )
                )
           )
    )
  )
(defn points-per-four [group player]
  (let [fmap (frequencies group) opponent (switch-player player)]
    (if (and (contains? fmap player) (not (contains? fmap opponent)))
      (get move-values (get fmap player))
      (if (and (contains? fmap opponent) (not (contains? fmap player)))
        (* -1 (get move-values (get fmap opponent)))
        0
        )
      )
    )
  )
(defn utility [state player]
  (apply + (map #(points-per-four % player) (concat (get-column-fours state) (get-row-fours state) (get-diagonal-fours state))))
  )

(defn check-diag [state]
  (some true? (map #(when (not= (first %) 0) (apply = %))
                   (get-diagonal-fours state)
                   )
        )
  )
(defn check-column [state]
  (some true?
        (map #(when (not= (first %) 0) (apply = %))
             (get-column-fours state))
        )
  )
(defn check-row [state]
  (some true?
        (map #(when (not= (first %) 0) (apply = %))
             (get-row-fours state))
        )
  )
(defn end-game [state]
  ;; Check each column for an end state
  (or
    (check-row state)
    (check-column state)
    (check-diag state)
    )
  ;; Check each row for end state

  )
(defn random-move [state player]
  (when (not (end-game state))
    (loop [states (get-valid-moves state player) num (rand-int (count states))]
      (if-not (nil? (get states num))
        num
        (recur (dissoc states num) (rand-int (count states)))
        )
      )
    )
  )

(defn min-r [state alpha beta depth curr-player player]
  (if (or (nil? state) (end-game state) (= depth MAXDEPTH))
    ;; If max depth, return the utility of this state.
    (utility state player)
    (let [state-map (get-valid-moves state curr-player) size (count state-map)]
      (loop [v 100000 b-val beta current 0]
        (let [
              state (get state-map current)
              x (min v (max-r state alpha beta (inc depth) (switch-player curr-player) player))
              newbeta (min b-val x)
              ]
          (if (or (<= b-val alpha) (= size (- current 1)))
            (do
              ;;(println "pruned")
              x)
            (recur x newbeta (inc current))
            )
          )
        )
      )
    )
  )
(defn max-r [state alpha beta depth curr-player player]
  (if (or (nil? state) (end-game state) (= depth MAXDEPTH))
    ;; If max depth, return the utility of this state.
    (utility state player)
    (let [state-map (get-valid-moves state curr-player) size (count state-map)]
      (loop [v -100000 a-val alpha current 0]
        (let [
              state (get state-map current)
              x (max v (min-r state alpha beta (inc depth) (switch-player curr-player) player))
              newalpha (max a-val x)
              ]
          (if (or (<= beta newalpha) (= size (- current 1)))
            (do
              ;;(println "pruned")
              x)
            (recur x newalpha (inc current))
            )
          )
        )

      )

    )
  )
(defn find-move [utilityval state player]
  (let [moves (get-valid-moves state player)]
    (loop [ind 0]
      (let [currentVal (utility (get moves ind) player)]
        (if (not= utilityval currentVal)
          (recur (inc ind))
          (do
            (binding [*out* *err*]
              ;;(println (get moves ind))
              )
            ind
            )
          )
        )
      )
    )
  )
(defn -main
  [& args]
  (loop []
    (flush)
    (let [gamestring (receive-game-string)]
      (if-not (nil? gamestring)
        (let [board (get gamestring "grid") width (get gamestring "width") height (get gamestring "height") player (get gamestring "player")]
          (let [string (generate-string {:move 3})]
            (binding [*out* *err*]
              ;;(println string)
              )
            (println string)
            )
          )
        )
      )
    ;; Bind 'gamestring' to the string parsed from input.

    (recur)
    )
  )





