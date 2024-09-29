package com.example.newsplus.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.newsplus.models.Article
import com.example.newsplus.models.NewsResponse
import com.example.newsplus.repository.NewsRepository
import com.example.newsplus.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Query
import java.io.IOException
import java.lang.NullPointerException
import java.util.Locale.IsoCountryCode

class NewsViewModel(app: Application,val newsRepository: NewsRepository):AndroidViewModel(app) {

    val headlines:MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var headlinesPage=1
    var headlinesResponse:NewsResponse?=null

    val searchNews:MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var searchNewsPage=1
    var searchNewsResponse:NewsResponse?=null
    var searchQuery: String?=null
    var newSeachQuery:String?=null
    var oldSearchQuery:String?=null

    init {
        getHeadlines("us")
    }


    fun getHeadlines(countryCode: String)=viewModelScope.launch {
        headlinesInternet(countryCode)
    }
    fun searchNews(searchQuery: String)=viewModelScope.launch {
        searchNewsInternet(searchQuery)
    }



    private fun handleHeadlinesResponse(response: Response<NewsResponse>):Resource<NewsResponse>{
        if(response.isSuccessful){
            response.body()?.let {resultResponse->
                headlinesPage++
                if(headlinesResponse==null){
                    headlinesResponse=resultResponse
                }else{
                    val oldArticles=headlinesResponse?.articles
                    val newArticles= resultResponse.articles
                    oldArticles?.addAll(newArticles)

                }
                return Resource.Success(headlinesResponse?:resultResponse)


            }
        }
        return Resource.Error(response.message())
    }

    private fun handleSearchNewsResponse(response: Response<NewsResponse>):Resource<NewsResponse>{
        if(response.isSuccessful){
            response.body()?.let { resultResponse->
                if(searchNewsResponse==null||newSeachQuery!=oldSearchQuery){
                    searchNewsPage=1
                    oldSearchQuery=newSeachQuery
                    searchNewsResponse=resultResponse
                }else{
                    searchNewsPage++
                    val oldArticles=searchNewsResponse?.articles
                    val newArticles=resultResponse.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(searchNewsResponse?:resultResponse )
            }
        }
        return Resource.Error(response.message())
    }
    fun addToFavourites(article: Article)= viewModelScope.launch{
        newsRepository.upsert(article)

    }
    fun getFavouriteNews()=newsRepository.getFavouriteNews()


  fun deleteArticle(article: Article)= viewModelScope.launch{
        newsRepository.deleteArticle(article)
    }
    fun internetConnection(context: Context):Boolean{
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
            return getNetworkCapabilities(activeNetwork)?.run {
                when{
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->true
                    else->false
                }
            }?:false
        }
    }
    private suspend fun headlinesInternet(countryCode:String){
        headlines.postValue(Resource.Loading())
        try {
            if(internetConnection(this.getApplication())){
                val response=newsRepository.getHeadlines( countryCode,headlinesPage)
                headlines.postValue(handleHeadlinesResponse(response))

            }else{
                headlines.postValue(Resource.Error("No internet"))
            }
        }catch (t:Throwable){
            when(t){
                is IOException->headlines.postValue(Resource.Error("Unable to connect"))
                else->headlines.postValue(Resource.Error("No Signal"))
            }
        }
    }
    private suspend fun searchNewsInternet(searchQuery: String){
        newSeachQuery=searchQuery
        searchNews.postValue(Resource.Loading())
        try{
            if(internetConnection(this.getApplication())){
                val response=newsRepository.searchNews(searchQuery,searchNewsPage)
                searchNews.postValue(handleSearchNewsResponse(response))
            }else{
                searchNews.postValue(Resource.Error("No internet"))
            }
        }catch (t:Throwable){
            when(t){
                is IOException->searchNews.postValue(Resource.Error("unable to connect"))
                else->searchNews.postValue(Resource.Error("No signal"))
            }
        }
    }
}