package com.example.newsplus.repository

import androidx.room.Query
import com.example.newsplus.api.RetrofitInstance
import com.example.newsplus.db.ArticleDatabase
import com.example.newsplus.models.Article
import java.util.Locale.IsoCountryCode

class NewsRepository(val db:ArticleDatabase) {
    suspend fun getHeadlines(countryCode: String,pageNumber:Int)=
        RetrofitInstance.api.getHeadlines(countryCode, pageNumber)


    suspend fun searchNews(searchQuery: String,pageNumber: Int)=
        RetrofitInstance.api.searchForNews(searchQuery, pageNumber)

    suspend fun upsert(article: Article)=db.getArticleDao().upsert(article)

    fun getFavouriteNews()=db.getArticleDao().getAllArticles()

    suspend fun deleteArticle(article: Article)=db.getArticleDao().deleteArticle(article)

}