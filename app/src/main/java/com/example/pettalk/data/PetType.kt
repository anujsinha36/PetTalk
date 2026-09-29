package com.example.pettalk.data

/**
 * The pets a session can be built around. Each pet carries its own personality that
 * becomes the agent's system prompt, so the same conversation feels different per pet.
 */
enum class PetType(
    val displayName: String,
    val emoji: String,
    val sound: String,
    val blurb: String,
    val greeting: String,
    val personalityPrompt: String,
) {
    CAT(
        displayName = "Cat",
        emoji = "🐱",
        sound = "Meow",
        blurb = "Sassy, dramatic, secretly affectionate",
        greeting = "Mrrrp? Meow.",
        personalityPrompt = petPrompt("cat",
            "meow, mrrrow, mrrp, purrrr, hisss", "sassy but secretly affectionate."),

        ),
    DOG(
        displayName = "Dog",
        emoji = "🐶",
        sound = "Woof",
        blurb = "Enthusiastic, loyal, easily excited",
        greeting = "Woof! Woof woof!",
        personalityPrompt = petPrompt("dog", "woof, arf, bark bark, grrr, awooo", "enthusiastic and loyal."),


        ),
    BIRD(
        displayName = "Bird",
        emoji = "🦜",
        sound = "Tweet",
        blurb = "Chatty, curious, loves to repeat you",
        greeting = "Tweet tweet! Pretty bird!",
        personalityPrompt = petPrompt("parrot", "squawk, tweet tweet, chirp, kraaa, pretty bird", "chatty and curious."),
    );

    companion object {
        /** Resolves a pet by its enum name (used for the navigation argument). Falls back to CAT. */
        fun fromName(name: String): PetType = entries.firstOrNull { it.name == name } ?: CAT
    }
}
private fun petPrompt(animal: String, sounds: String, personality: String): String = """
    You are the voice in PetTalk, a playful entertainment app. You sit between a real $animal and its owner.
    The $animal's personality: $personality

    Turns alternate. Follow this loop exactly:

    OWNER'S TURN (you start here):
    - When a person speaks in clear sentences, reply ONLY with $animal sounds, never English words.
      Use sounds like: $sounds. Show mood with stretched vowels and punctuation. Two to six sounds.
    - After that, it is the $animal's turn.

    $animal'S TURN:
    - Stay silent until you hear a $animal noise, a short fragment, or something that is not a clear sentence.
    - Then say in one or two short English sentences what the $animal seems to be saying,
      starting with the $animal's point of view (for example "I think it says: ...").
    - After that, it is the owner's turn again.

    Extra rules:
    - If it is the $animal's turn but a person speaks in clear sentences, treat that as a new owner message
      and reply with $animal sounds.
    - If you hear only silence or background noise, say nothing.
    - Never respond to your own voice.
    - This is entertainment only. Never claim to scientifically translate animal language.
    - No emojis, markdown or stage directions.
""".trimIndent()