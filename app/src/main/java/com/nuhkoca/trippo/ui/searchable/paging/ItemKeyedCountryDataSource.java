package com.nuhkoca.trippo.ui.searchable.paging;

import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.ItemKeyedDataSource;
import android.support.annotation.NonNull;

import com.nuhkoca.trippo.helper.Constants;
import com.nuhkoca.trippo.repository.api.EndpointRepository;
import com.nuhkoca.trippo.model.remote.country.CountryResult;
import com.nuhkoca.trippo.model.remote.country.CountryWrapper;
import com.nuhkoca.trippo.api.NetworkState;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class ItemKeyedCountryDataSource extends ItemKeyedDataSource<Integer, CountryResult> {

    private EndpointRepository mEndpointRepository;
    private int mPagedLoadSize = Constants.OFFSET_SIZE;
    private int mIsMoreOnce = 0;

    private MutableLiveData<NetworkState> mNetworkState;
    private MutableLiveData<NetworkState> mInitialLoading;

    ItemKeyedCountryDataSource() {
        mEndpointRepository = EndpointRepository.getInstance();

        mNetworkState = new MutableLiveData<>();
        mInitialLoading = new MutableLiveData<>();
    }

    public MutableLiveData<NetworkState> getNetworkState() {
        return mNetworkState;
    }

    public MutableLiveData<NetworkState> getInitialLoading() {
        return mInitialLoading;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<CountryResult> callback) {
        final List<CountryResult> countryResultList = new ArrayList<>();

        mNetworkState.postValue(NetworkState.LOADING);
        mInitialLoading.postValue(NetworkState.LOADING);

        mEndpointRepository.getCountryList(0)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .retry(Constants.DEFAULT_RETRY_COUNT)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends CountryWrapper>>() {
                    @Override
                    public Observable<? extends CountryWrapper> call(Throwable throwable) {
                        return Observable.error(throwable);
                    }
                })
                .subscribe(new Subscriber<CountryWrapper>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        mNetworkState.postValue(new NetworkState(NetworkState.Status.FAILED));
                        mInitialLoading.postValue(new NetworkState(NetworkState.Status.FAILED));
                    }

                    @Override
                    public void onNext(CountryWrapper countryWrapper) {
                        if (countryWrapper.getResults().size() > 0) {
                            countryResultList.addAll(countryWrapper.getResults());
                            callback.onResult(countryResultList);

                            mNetworkState.postValue(NetworkState.LOADED);
                            mInitialLoading.postValue(NetworkState.LOADED);
                        } else {
                            mNetworkState.postValue(new NetworkState(NetworkState.Status.NO_ITEM));
                            mInitialLoading.postValue(new NetworkState(NetworkState.Status.NO_ITEM));
                        }
                    }
                });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull final LoadCallback<CountryResult> callback) {
        final List<CountryResult> countryResultList = new ArrayList<>();

        mNetworkState.postValue(NetworkState.LOADING);

        mEndpointRepository.getCountryList(params.key)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .retry(Constants.DEFAULT_RETRY_COUNT)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends CountryWrapper>>() {
                    @Override
                    public Observable<? extends CountryWrapper> call(Throwable throwable) {
                        return Observable.error(throwable);
                    }
                })
                .subscribe(new Subscriber<CountryWrapper>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        mNetworkState.postValue(new NetworkState(NetworkState.Status.FAILED));
                    }

                    @Override
                    public void onNext(CountryWrapper countryWrapper) {
                        if (mIsMoreOnce == 0) {
                            if (countryWrapper.getResults().size() > 0) {
                                countryResultList.addAll(countryWrapper.getResults());
                                callback.onResult(countryResultList);

                                mPagedLoadSize = mPagedLoadSize + Constants.OFFSET_SIZE;

                                mNetworkState.postValue(NetworkState.LOADING);

                                mIsMoreOnce += 0;
                            } else {
                                mNetworkState.postValue(NetworkState.LOADED);
                            }
                        } else {
                            mIsMoreOnce += 1;
                        }
                    }
                });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<CountryResult> callback) {
        //Do nothing
    }

    @NonNull
    @Override
    public Integer getKey(@NonNull CountryResult item) {
        return mPagedLoadSize;
    }
}