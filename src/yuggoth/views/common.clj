(ns yuggoth.views.common
  (:use hiccup.element hiccup.form
        [noir.core]
        [noir.validation :as vali]        
        [hiccup.page :only [include-css include-js html5]])
  (:require [yuggoth.views.util :as util]
            [yuggoth.models.db :as db]
            [noir.session :as session]))

(defn header []
  [:div.header [:h1 [:div.site-title  (:title (db/get-admin))]]])

(defn menu []
  [:div.menu 
   (into
     (if (session/get :admin) 
       [:ul.menu-items          
        [:li (link-to "/logout" "logout")]
        [:li (link-to "/profile" "profile")]
        [:li (link-to "/upload" "upload")]
        [:li#new-post (link-to "/make-post" "New post")]]
       [:ul.menu-items])     
     [[:li#rss (link-to "/rss" [:div#rss "rss"] (image "/img/rss.jpg"))]      
      [:li#about (link-to "/about" "About")]      
      [:li#archives (link-to "/archives" "Archives")]
      [:li#home (link-to "/" "Home")]])])

(defn sidebar [title]
  (if (or (= "New post" title) (= "Edit post" title))    
    [:div.sidebar-preview
     [:h2 [:span.render-preview "Preview (click to redraw)"]]
     [:div#post-preview]]
    
    [:div.sidebar
     [:h2 "Recent posts"]     
     (-> [:ul]       
       (into 
         (for [{:keys [id time title]} (reverse (sort-by :time (db/get-posts 5)))]
           [:li 
            (link-to (str "/blog/" id)
                     title
                     [:div.date (util/format-time time)])]))
       (conj [:li (link-to "/archives" "more...")]))]))

(defn footer []
  [:div.footer
   [:p "Copyright (C) 2012 " 
    (:handle (db/get-admin)) 
    (when (not (session/get :admin)) [:span " (" (link-to "/login" "login") ")"]) 
    " - Powered by: "
    (link-to "http://github.com/yogthos/yuggoth" "Yuggoth")]])

(defpartial layout [title & content]
  (let [html-title (if (string? title) title (:title title))
        title-elements (when (map? title) (:elements title))]        
    (html5
      [:head
       [:title html-title]
       (include-css (util/get-css)
                    "/css/jquery.alerts.css")]      
      [:body
       (hidden-field "selected" 
                     (condp = html-title
                       "Archives" "#archives"
                       "Login" "#login"
                       "New post" "#new-post"
                       "#home"))
       [:div.container
        (header)
        (menu)
        [:div.contents 
         [:div.post
          [:div.entry-title [:h2 html-title title-elements]]
          [:div.entry-content content]]
         (sidebar html-title)]          
        (footer)]
       (include-js "/js/markdown.js"
                   "https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"
                   "/js/jquery.alerts.js"
                   "/js/site.js")])))
