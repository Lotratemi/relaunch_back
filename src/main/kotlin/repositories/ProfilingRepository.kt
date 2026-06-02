package com.codingfactory.repositories

import com.codingfactory.Database
import com.codingfactory.Database.exec
import com.codingfactory.Database.fetchAll
import com.codingfactory.Database.fetchOne
import com.codingfactory.models.ProfilingModels.ProfileDimension
import com.codingfactory.models.ProfilingModels.ProfilingAnswer
import com.codingfactory.models.ProfilingModels.UserProfile
import io.github.jan.supabase.postgrest.from

class ProfilingRepository {

    // ─── Sauvegarder le profil synthétique ──────────────────────────────

    suspend fun saveProfile(profile: UserProfile): UserProfile? = Database.run(
        prod = {
            it.from("user_profiles").insert(profile) { select() }.decodeSingleOrNull()
        },
        test = {
            it.fetchOne(
                """
                INSERT INTO user_profiles (user_id, profile_type, version, raw_scores)
                VALUES (?, ?, ?, ?::jsonb)
                RETURNING to_jsonb(user_profiles.*) || jsonb_build_object('raw_scores', raw_scores::text)
                """.trimIndent(),
                profile.userId, profile.profileType, profile.version, profile.rawScores
            )
        }
    )

    // ─── Sauvegarder les réponses brutes ────────────────────────────────

    suspend fun saveAnswers(answers: List<ProfilingAnswer>): Unit = Database.run(
        prod = {
            it.from("profiling_answers").insert(answers)
            Unit
        },
        test = {
            answers.forEach { answer ->
                it.exec(
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
    )

    // ─── Sauvegarder les dimensions calculées ───────────────────────────

    suspend fun saveDimensions(dimensions: List<ProfileDimension>): Unit = Database.run(
        prod = {
            it.from("profile_dimensions").insert(dimensions)
            Unit
        },
        test = {
            dimensions.forEach { dim ->
                it.exec(
                    """
                    INSERT INTO profile_dimensions (user_profile_id, dimension, score, label)
                    VALUES (?, ?, ?, ?)
                    """.trimIndent(),
                    dim.userProfileId, dim.dimension, dim.score, dim.label
                )
            }
            Unit
        }
    )

    // ─── Récupérer le dernier profil d'un utilisateur ───────────────────

    suspend fun findLatestByUserId(userId: Long): UserProfile? = Database.run(
        prod = {
            it.from("user_profiles").select {
                filter { UserProfile::userId eq userId }
                order("completed_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }.decodeSingleOrNull()
        },
        test = {
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
    )

    // ─── Récupérer les dimensions d'un profil ───────────────────────────

    suspend fun findDimensionsByProfileId(profileId: Long): List<ProfileDimension> = Database.run(
        prod = {
            it.from("profile_dimensions").select {
                filter { ProfileDimension::userProfileId eq profileId }
            }.decodeList()
        },
        test = {
            it.fetchAll(
                "SELECT to_jsonb(d) FROM profile_dimensions d WHERE user_profile_id = ?",
                profileId
            )
        }
    )

    // ─── Historique complet d'un utilisateur ────────────────────────────

    suspend fun findHistoryByUserId(userId: Long): List<UserProfile> = Database.run(
        prod = {
            it.from("user_profiles").select {
                filter { UserProfile::userId eq userId }
                order("completed_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList()
        },
        test = {
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
    )

    // ─── Réponses brutes d'un profil ────────────────────────────────────

    suspend fun findAnswersByProfileId(profileId: Long): List<ProfilingAnswer> = Database.run(
        prod = {
            it.from("profiling_answers").select {
                filter { ProfilingAnswer::userProfileId eq profileId }
                order("question_id", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }.decodeList()
        },
        test = {
            it.fetchAll(
                """
                SELECT to_jsonb(a) FROM profiling_answers a
                WHERE user_profile_id = ?
                ORDER BY question_id ASC
                """.trimIndent(),
                profileId
            )
        }
    )
}