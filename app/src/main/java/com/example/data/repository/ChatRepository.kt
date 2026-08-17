package com.example.data.repository

import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    fun getAllSessions(): Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun getSessionById(sessionId: Long): ChatSessionEntity? =
        chatDao.getSessionById(sessionId)

    suspend fun getMessagesList(sessionId: Long): List<ChatMessageEntity> =
        chatDao.getMessagesListForSession(sessionId)

    suspend fun createNewSession(title: String = "New Transmission"): Long {
        val session = ChatSessionEntity(
            title = title,
            createdAt = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis(),
            messageCount = 0
        )
        return chatDao.insertSession(session)
    }

    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(title = newTitle, lastUpdated = System.currentTimeMillis()))
        }
    }

    suspend fun addMessage(
        sessionId: Long,
        sender: String,
        content: String,
        isStreaming: Boolean = false,
        isError: Boolean = false
    ): Long {
        val msg = ChatMessageEntity(
            sessionId = sessionId,
            sender = sender,
            content = content,
            timestamp = System.currentTimeMillis(),
            isStreaming = isStreaming,
            isError = isError
        )
        val msgId = chatDao.insertMessage(msg)

        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            val count = session.messageCount + 1
            val updatedTitle = if (session.title == "New Transmission" && sender == "user") {
                content.take(30) + (if (content.length > 30) "..." else "")
            } else {
                session.title
            }
            chatDao.updateSession(
                session.copy(
                    title = updatedTitle,
                    lastUpdated = System.currentTimeMillis(),
                    messageCount = count
                )
            )
        }

        return msgId
    }

    suspend fun updateMessageContent(
        messageId: Long,
        content: String,
        isStreaming: Boolean,
        isError: Boolean = false
    ) {
        chatDao.updateMessageContent(messageId, content, isStreaming, isError)
    }

    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun clearSessionMessages(sessionId: Long) {
        chatDao.clearMessagesForSession(sessionId)
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(messageCount = 0, lastUpdated = System.currentTimeMillis()))
        }
    }

    suspend fun clearAllData() {
        chatDao.deleteAllMessages()
        chatDao.deleteAllSessions()
    }
}
