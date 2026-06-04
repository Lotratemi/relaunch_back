package com.codingfactory.repositories

import com.codingfactory.Database
import com.codingfactory.Database.exec
import com.codingfactory.Database.fetchAll
import com.codingfactory.Database.fetchOne
import com.codingfactory.models.ProfilingModels.ProfileDimension
import com.codingfactory.models.ProfilingModels.ProfilingAnswer
import com.codingfactory.models.ProfilingModels.UserProfile

class ProfilingRepository {

    // ─── Sauvegarder le profil synthétique ──────────────────────────────

    suspend fun saveProfile(profile: UserProfile): UserProfile? = Database.run {
        it.fetchOne(
            """
            INSERT INTO user_profiles (user_id, profile_type, version, raw_scores)
            VALUES (?, ?, ?, ?::jsonb)
            RETURNING to_jsonb(user_profiles.*) || jsonb_build_object('raw_scores', raw_scores::text)
            """.trimIndent(),
            profile.userId, profile.profileType, profile.version, profile.rawScores
        )
    }

    // ─── Sauvegarder les réponses brutes ────────────────────────────────

    suspend fun saveAnswers(answers: List<ProfilingAnswer>): Unit = Database.run { conn ->
        answers.forEach { answer ->
            conn.exec(
                """
                INSERT INTO profiling_answers
                    (user_profile_id, question_id, question_text, answer_value)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                answer.userProfileId, answer.questionId,
                answer.questionText, answer.answerValue
            )
        }
        Unit
    }

    // ─── Sauvegarder les dimensions calculées ───────────────────────────

    suspend fun saveDimensions(dimensions: List<ProfileDimension>): Unit = Database.run { conn ->
        dimensions.forEach { dim ->
            conn.exec(
                """
                INSERT INTO profile_dimensions (user_profile_id, dimension, score, label)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                dim.userProfileId, dim.dimension, dim.score, dim.label
            )
        }
        Unit
    }

    // ─── Récupérer le dernier profil d'un utilisateur ───────────────────

    suspend fun findLatestByUserId(userId: Long): UserProfile? = Database.run {
        it.fetchOne(
            """
            SELECT to_jsonb(p) || jsonb_build_object('raw_scores', p.raw_scores::text)
            FROM user_profiles p
            WHERE user_id = ?
            ORDER BY completed_at DESC
            LIMIT 1
            """.trimIndent(),
            userId
        )
    }

    // ─── Récupérer les dimensions d'un profil ───────────────────────────

    suspend fun findDimensionsByProfileId(profileId: Long): List<ProfileDimension> = Database.run {
        it.fetchAll(
            "SELECT to_jsonb(d) FROM profile_dimensions d WHERE user_profile_id = ?",
            profileId
        )
    }

    // ─── Historique complet d'un utilisateur ────────────────────────────

    suspend fun findHistoryByUserId(userId: Long): List<UserProfile> = Database.run {
        it.fetchAll(
            """
            SELECT to_jsonb(p) || jsonb_build_object('raw_scores', p.raw_scores::text)
            FROM user_profiles p
            WHERE user_id = ?
            ORDER BY completed_at DESC
            """.trimIndent(),
            userId
        )
    }

    // ─── Réponses brutes d'un profil ────────────────────────────────────

    suspend fun findAnswersByProfileId(profileId: Long): List<ProfilingAnswer> = Database.run {
        it.fetchAll(
            """
            SELECT to_jsonb(a) FROM profiling_answers a
            WHERE user_profile_id = ?
            ORDER BY question_id ASC
            """.trimIndent(),
            profileId
        )
    }
}