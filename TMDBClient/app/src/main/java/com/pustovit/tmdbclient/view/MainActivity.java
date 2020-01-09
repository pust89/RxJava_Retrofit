package com.pustovit.tmdbclient.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.pustovit.tmdbclient.R;
import com.pustovit.tmdbclient.adapter.MovieAdapter;
import com.pustovit.tmdbclient.model.Movie;
import com.pustovit.tmdbclient.model.MovieDBResponse;
import com.pustovit.tmdbclient.service.MovieDataService;
import com.pustovit.tmdbclient.service.RetrofitInstance;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;

import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "myTag";

    private List<Movie> movies;
    private RecyclerView recyclerView;
    private MovieAdapter movieAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Call<MovieDBResponse> call;
    private Observable<MovieDBResponse> mMovieDBResponseObservable;
    private CompositeDisposable mCompositeDisposable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCompositeDisposable = new CompositeDisposable();
        recyclerView = findViewById(R.id.rvMovies);


        mSwipeRefreshLayout = findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //   getPopularMovies();
                getPopularMoviesWithRx();
            }
        });
        getSupportActionBar().setTitle("TMDB Popular movies today ");

        // getPopularMovies();
        getPopularMoviesWithRx();

    }

    //RxJava here
    private void getPopularMoviesWithRx() {
        MovieDataService movieDataService = RetrofitInstance.getService();

        mMovieDBResponseObservable = movieDataService.getPopularMoviesWithRx(getString(R.string.api_key));


//        mCompositeDisposable.add(
//                mMovieDBResponseObservable
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribeWith(new DisposableObserver<MovieDBResponse>() {
//                            @Override
//                            public void onNext(MovieDBResponse movieDBResponse) {
//                                movies = movieDBResponse.getMovies();
//                                Log.d(TAG, "onNext: movies.size() : " + movies.size());
//                            }
//
//                            @Override
//                            public void onError(Throwable e) {
//                            }
//                            @Override
//                            public void onComplete() {
//                                showOnRecyclerView();
//                            }
//                        })
//     );

        /*        **Realization with flatMap operator**  */
        if (movies == null) {
            movies = new ArrayList<>();
        } else {
            movies.clear();
        }
        mCompositeDisposable.add(
                mMovieDBResponseObservable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMap(new Function<MovieDBResponse, Observable<Movie>>() {
                            @Override
                            public Observable<Movie> apply(MovieDBResponse movieDBResponse) throws Exception {
                                return Observable.fromArray(movieDBResponse.getMovies().toArray(new Movie[0]));
                            }
                        })
                        .filter(new Predicate<Movie>() {
                            @Override
                            public boolean test(Movie movie) throws Exception {
                                return movie.getVoteAverage() > 6;// filtering rating
                            }
                        })
                        .subscribeWith(new DisposableObserver<Movie>() {
                            @Override
                            public void onNext(Movie movie) {
                                movies.add(movie);
                                Log.d(TAG, "onNext: add title \"" + movie.getTitle() + "\"");
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {
                                showOnRecyclerView();
                            }
                        })
        );


    }


    /*        **Realization only with Retrofit**  */
//    private void getPopularMovies() {
//        MovieDataService movieDataService = RetrofitInstance.getService();
//
//        call = movieDataService.getPopularMovies(getString(R.string.api_key));
//
//        call.enqueue(new Callback<MovieDBResponse>() {
//            @Override
//            public void onResponse(Call<MovieDBResponse> call, Response<MovieDBResponse> response) {
//
//                MovieDBResponse movieDBResponse = response.body();
//
//                if (movieDBResponse != null && movieDBResponse.getMovies() != null) {
//                    movies = movieDBResponse.getMovies();
//                    showOnRecyclerView();
//                }
//
//                Log.d(TAG, "onResponse: " + movieDBResponse.getMovies().size());
//            }
//
//            @Override
//            public void onFailure(Call<MovieDBResponse> call, Throwable t) {
//
//            }
//        });
//
//    }

    private void showOnRecyclerView() {
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        }

        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setItemViewCacheSize(20);
        movieAdapter = new MovieAdapter(this, movies);
        recyclerView.setAdapter(movieAdapter);
        movieAdapter.notifyDataSetChanged();


        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();

        /*        **Realization only with Retrofit**  */
//        if(call != null){
//            if(call.isExecuted()){
//                call.cancel();
//            }
//        }
    }
}
