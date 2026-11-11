package com.saurabhsvision.keyboard;

/**
 * Offline Unicode emoji collection. All emoji keys insert directly into the
 * foreground Android text field. Actual glyph colors/fonts depend on Android.
 */
public final class EmojiCatalog {
    private EmojiCatalog(){}
    public static final String[] CATEGORIES={"Faces","Love","Feelings","Hands","People","Animals","Nature","Food","Fun","Symbols","Science"};
    private static final String[] RAW={
        "😀 😃 😄 😁 😆 😅 🤣 😂 🙂 🙃 🫠 😉 😊 😇 🥰 😍 🤩 😘 😗 ☺️ 😚 😙 🥲 😋 😛 😜 🤪 😝 🤑 🤗 🤭 🫢 🫣 🤫 🤔 🫡 🤐 🤨 😐 😑 😶 🫥 😶‍🌫️ 😏 😒 🙄 😬 😮‍💨 🤥 🫨 😌 😔 😪 🤤 😴 🥱 😷 🤒 🤕 🤢 🤮 🤧 🥵 🥶 🥴 😵 😵‍💫 🤯 🤠 🥳 🥸 😎 🤓 🧐 😕 🫤 🙁 ☹️ 😮 😯 😲 😳 🥺 🥹 😦 😧 😨 😰 😥 😢 😭 😱 😖 😣 😞 😓 😩 😫 😤 😡 😠 🤬 😈 👿 💀 ☠️ 💩 🤡 👹 👺 👻 👽 👾 🤖 😺 😸 😹 😻 😼 😽 🙀 😿 😾",
        "❤️ 🩷 🧡 💛 💚 💙 🩵 💜 🤎 🖤 🩶 🤍 💖 💗 💓 💞 💕 💟 ❣️ 💔 ❤️‍🔥 ❤️‍🩹 💘 💝 💌 🫶 🥰 😍 😘 😚 😙 😗 🥲 😊 😇 🤗 🤭 💋 👩‍❤️‍👨 👨‍❤️‍👨 👩‍❤️‍👩 👩‍❤️‍💋‍👨 👨‍❤️‍💋‍👨 👩‍❤️‍💋‍👩 💑 💏 💍 💎 🌹 🥀 🌷 🌺 🌸 💐 🧸 🎁 💒 💫 ✨ 🎀 🍫 🕊️ 🤝 🫂 🫰",
        "😭 😢 😞 😔 🥹 🥺 😿 💔 ❤️‍🩹 😤 😠 😡 🤬 😈 👿 😣 😖 😩 😫 🥱 😴 😪 😥 😓 😰 😨 😱 🫣 😳 😬 😐 😑 🙄 😒 😕 🫤 🙁 ☹️ 😮 😯 😲 🫨 😵 😵‍💫 🤯 🤔 🧐 🤨 🤥 🤫 🤭 🫢 🤗 🤩 🥳 😎 😇 😌 🤤 🤒 🤕 😷 🤧 🤢 🤮 🥴 🥶 🥵 😅 🤣 😂 😆 😄 🥰 😍 🙂 🙃 😉 😶 🫥",
        "👋 🤚 🖐️ ✋ 🖖 🫱 🫲 🫳 🫴 🫷 🫸 👌 🤌 🤏 ✌️ 🤞 🫰 🤟 🤘 🤙 👈 👉 👆 🖕 👇 ☝️ 🫵 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 🫶 👐 🤲 🤝 🙏 ✍️ 💪 🦾 🫂 👏🏻 👏🏼 👏🏽 👏🏾 👏🏿 👍🏻 👍🏼 👍🏽 👍🏾 👍🏿 👋🏻 👋🏼 👋🏽 👋🏾 👋🏿 🙏🏻 🙏🏼 🙏🏽 🙏🏾 🙏🏿 🤞🏻 🤞🏼 🤞🏽 🤞🏾 🤞🏿 💅 👂 👃 🦶 🦵 👀 👁️ 👅 👄 🫦",
        "🧒 👶 👧 👦 👩 👨 🧑 👱 🧔 👵 👴 🧓 👨‍🎓 👩‍🎓 🧑‍🎓 👩‍🏫 👨‍🏫 🧑‍🏫 👩‍🔬 👨‍🔬 🧑‍🔬 👩‍💻 👨‍💻 🧑‍💻 👩‍🚀 👨‍🚀 🧑‍🚀 👩‍⚕️ 👨‍⚕️ 🧑‍⚕️ 👷 👮 🕵️ 💂 🥷 🦸 🦹 🧙 🧚 🧛 🧜 🧝 🧟 🙍 🙎 🙅 🙆 💁 🙋 🧏 🙇 🤦 🤷 💆 💇 🚶 🏃 🧍 🧎 🧘 🛌 🕺 💃 👯 🫂",
        "🐶 🐕 🦮 🐕‍🦺 🐩 🐺 🦊 🦝 🐱 🐈 🐈‍⬛ 🦁 🐯 🐅 🐆 🐴 🫎 🫏 🦄 🦓 🦌 🦬 🐮 🐂 🐃 🐄 🐷 🐖 🐗 🐽 🐏 🐑 🐐 🐪 🐫 🦙 🦒 🐘 🦣 🦏 🦛 🐭 🐁 🐀 🐹 🐰 🐇 🐿️ 🦫 🦔 🦇 🐻 🐻‍❄️ 🐨 🐼 🦥 🦦 🦨 🦘 🦡 🐾 🦃 🐔 🐓 🐣 🐤 🐥 🐦 🐧 🕊️ 🦅 🦆 🦢 🦉 🦩 🦚 🦜 🐸 🐊 🐢 🦎 🐍 🐲 🐉 🐳 🐬 🦈 🐙 🪼 🦀 🐠 🐟 🐡 🦋 🐛 🐝 🐞 🐜 🕷️",
        "🌸 🌷 🌹 🥀 🌺 🌻 🌼 🪷 💐 🌱 🪴 🌲 🌳 🌴 🌵 🌾 🍀 ☘️ 🍁 🍂 🍃 🌿 🪻 🍄 🪹 🪺 🪨 🪵 🌎 🌍 🌏 🌐 🌑 🌒 🌓 🌔 🌕 🌖 🌗 🌘 🌙 🌚 🌛 🌜 🌝 🌞 ⭐ 🌟 🌠 🌌 🌈 ☀️ 🌤️ ⛅ 🌥️ ☁️ 🌦️ 🌧️ ⛈️ 🌩️ 🌨️ ❄️ ☃️ ⛄ 🌬️ 💨 💧 💦 ☔ ☂️ 🌊 🌫️ 🌪️ 🌡️ 🔥 💥 🌋 🏔️ ⛰️ 🏞️ 🌅 🌄",
        "🍎 🍏 🍐 🍊 🍋 🍋‍🟩 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥬 🥒 🌶️ 🫑 🌽 🥕 🫒 🧄 🧅 🥔 🍠 🫘 🥜 🌰 🥐 🍞 🥖 🥨 🥯 🥞 🧇 🧀 🍖 🍗 🥩 🥓 🍔 🍟 🍕 🌭 🥪 🌮 🌯 🥙 🧆 🥚 🍳 🥘 🍲 🥣 🥗 🍿 🧈 🧂 🍱 🍚 🍛 🍜 🍝 🍣 🍤 🍩 🍪 🎂 🍰 🧁 🍫 🍬 🍭 🍮 🍯 🍼 🥛 ☕ 🫖 🍵 🧃 🥤 🧋 🍹",
        "⚽ 🏀 🏈 ⚾ 🥎 🎾 🏐 🏉 🥏 🎱 🪀 🏓 🏸 🏒 🏑 🥍 🏏 🪃 🥅 ⛳ 🪁 🏹 🎣 🤿 🥊 🥋 🎽 🛹 🛼 🛷 ⛸️ 🥌 🎿 ⛷️ 🏂 🪂 🏋️ 🤸 ⛹️ 🤾 🏌️ 🏇 🚴 🏊 🤽 🚣 🧗 🎮 🕹️ 🎲 ♟️ 🎯 🎳 🪄 🎸 🎹 🎺 🎷 🥁 🪕 🎻 🎤 🎧 🎼 🎵 🎶 🎨 🎬 🎭 🎪 🎟️ 🎫 🏆 🥇 🥈 🥉 🎖️ 🎊 🎉 🪅 🎈 🎁 🎀",
        "✅ ☑️ ✔️ ❌ ❎ ➕ ➖ ✖️ ➗ 🟰 ⁉️ ❗ ❕ ❓ ❔ 💯 🔥 💫 ✨ 🌟 ⭐ 💥 ❤️ 💔 💕 ☮️ ☯️ ☪️ ✝️ ☸️ ✡️ 🕉️ 🔯 🪯 ♈ ♉ ♊ ♋ ♌ ♍ ♎ ♏ ♐ ♑ ♒ ♓ 🆗 🆒 🆕 🆙 🆓 🆘 🆚 🔜 🔝 🔛 🔚 🔙 ➡️ ⬅️ ⬆️ ⬇️ ↗️ ↘️ ↙️ ↖️ ↔️ ↕️ 🔄 🔃 ♻️ 🔁 🔂 ▶️ ⏸️ ⏹️ 🔊 🔉 🔈 🔇 🔔 🔕 💬 💭 🗯️ 💤 💢 💝",
        "🧮 📐 📏 📊 📈 📉 🔢 🔣 🧪 ⚗️ 🧫 🧬 🔬 🔭 ⚛️ 🧲 💡 🧠 🫀 🫁 🦴 🦷 🩸 💉 🩺 🩻 💊 🧯 🪐 🌌 🚀 🛰️ ☄️ 🌍 🌎 🌏 🔋 ⚡ 💻 🖥️ 🖨️ ⌨️ 🖱️ 💾 💿 📀 🕹️ 📡 🤖 📱 📲 🧑‍🔬 👩‍🔬 👨‍🔬 🧑‍🚀 👩‍🚀 👨‍🚀 📚 📖 📝 ✏️ 📒 📔 🧾 🎓 🏫 🏅"
    };
    private static final String[][] CACHE=new String[RAW.length][];
    public static int count(){return CATEGORIES.length;}
    public static String title(int group){return CATEGORIES[Math.max(0,Math.min(CATEGORIES.length-1,group))];}
    public static String[] items(int group){
        int n=Math.max(0,Math.min(RAW.length-1,group));
        if(CACHE[n]==null)CACHE[n]=RAW[n].trim().split(" +");
        return CACHE[n];
    }
    public static int total(){
        int n=0;for(int i=0;i<RAW.length;i++)n+=items(i).length;
        return n;
    }
}
