package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val service: ApiService,
    private val db: AppDb,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        return try {
            val response = when (loadType) {
                LoadType.REFRESH -> {
                    val afterKey = postRemoteKeyDao.maxByType(PostRemoteKeyEntity.KeyType.AFTER)
                    if (afterKey == null) {
                        service.getLatest(state.config.initialLoadSize)
                    } else {
                        service.getAfter(afterKey, state.config.initialLoadSize)
                    }
                }
                LoadType.PREPEND ->
                    return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val beforeKey = postRemoteKeyDao.minByType(PostRemoteKeyEntity.KeyType.BEFORE)
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    service.getBefore(beforeKey, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body().orEmpty()

            db.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        val firstId = body.firstOrNull()?.id ?: return@withTransaction
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                type = PostRemoteKeyEntity.KeyType.AFTER,
                                id = firstId
                            )
                        )
                    }
                    LoadType.APPEND -> {
                        val lastId = body.lastOrNull()?.id ?: return@withTransaction
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                type = PostRemoteKeyEntity.KeyType.BEFORE,
                                id = lastId
                            )
                        )
                    }
                    else -> { /* PREPEND пропущен */ }
                }
                postDao.insert(body.toEntity())
            }

            MediatorResult.Success(endOfPaginationReached = body.isEmpty())
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
