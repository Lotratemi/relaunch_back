package com.codingfactory

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

val supabase = createSupabaseClient(
    supabaseUrl = System.getenv("SUPABASE_URL")
        ?: error("Missing SUPABASE_URL environment variable"),
    supabaseKey = System.getenv("SUPABASE_KEY")
        ?: error("Missing SUPABASE_KEY environment variable")
) {
    install(Postgrest)
    install(Auth)
    install(Realtime)
}
