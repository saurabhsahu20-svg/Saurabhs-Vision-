package com.saurabhsvision.sbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * Offline school assembly & games. Live headlines are retrieved on demand,
 * with attributed sources, true publication times and honest error states.
 */
public final class DailyAssemblyActivity extends Activity {
    private static final int BG=0xff081327,PANEL=0xff132942,EDGE=0xff365475,
        WHITE=0xfff3f7ff,MUTED=0xffa9bdd8,MINT=0xff7eeac0,
        CYAN=0xff64e1f6,GOLD=0xffffd28f;
    private static final String PREFS="s_browser_daily_assembly";
    private LocalDate today=LocalDate.now(ZoneId.systemDefault());
    private int scene=0,newsRequest=0,practiceOffset=0;
    private boolean hindi=true,cgNews=true,closed=false;
    private int newsDays=1,zodiac=0;
    private LinearLayout page;
    private TextView pageStatus;
    private ProgressBar newsProgress;
    private EditText[] cells;
    private TextToSpeech speechEngine;
    private boolean readyToSpeak=false;

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable background(int color,int stroke,int radius){
        GradientDrawable v=new GradientDrawable();
        v.setColor(color);v.setCornerRadius(dp(radius));
        if(stroke!=0)v.setStroke(dp(1),stroke);
        return v;
    }
    private TextView text(String s,int size,int color,boolean medium){
        TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.create(medium?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        t.setLineSpacing(dp(3),1.03f);
        return t;
    }
    private LinearLayout vertical(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
    private LinearLayout horizontal(){
        LinearLayout h=new LinearLayout(this);h.setOrientation(0);h.setGravity(Gravity.CENTER_VERTICAL);
        return h;
    }
    private void add(LinearLayout parent,View child,int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(margin);parent.addView(child,p);
    }
    private TextView button(String label,Runnable action){
        TextView t=text(label,14,WHITE,true);
        t.setGravity(Gravity.CENTER);t.setPadding(dp(11),dp(11),dp(11),dp(11));
        t.setBackground(background(PANEL,EDGE,15));
        t.setOnClickListener(v->action.run());return t;
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void share(String content){
        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT,content+"\n\n— S Browser • SAURABH'S VISION™");
        startActivity(Intent.createChooser(send,"Share assembly content"));
    }
    private void copy(String content){
        ClipboardManager clipboard=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        if(clipboard!=null){
            clipboard.setPrimaryClip(ClipData.newPlainText("S Browser Assembly",content));
            toast("Copied to clipboard");
        }
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        android.content.SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
        hindi=p.getBoolean("hindi",true);
        cgNews=p.getBoolean("cg",true);
        zodiac=p.getInt("zodiac",0);
        speechEngine=new TextToSpeech(this,status->{
            readyToSpeak=(status==TextToSpeech.SUCCESS);
        });
        show(0);
    }
    private void remember(){
        getSharedPreferences(PREFS,MODE_PRIVATE).edit()
            .putBoolean("hindi",hindi).putBoolean("cg",cgNews)
            .putInt("zodiac",zodiac).apply();
    }
    private String pageTitle(){
        switch(scene){
            case 1:return "Thought of the Day";
            case 2:return "School Assembly Speech";
            case 3:return "Sports News";
            case 4:return "General & Latest News";
            case 5:return "Astrology & Reflection";
            case 6:return "Mini Crossword";
            case 7:return "Puzzle Games";
            case 8:return "Morning Assembly Script";
            default:return "Daily Assembly & Fun";
        }
    }
    private void show(int newScene){
        scene=newScene;
        newsRequest++; // stale news fetch must never overwrite another page
        cells=null;
        LinearLayout root=vertical();
        root.setBackgroundColor(BG);
        root.setPadding(dp(14),dp(9),dp(14),dp(13));
        setContentView(root);
        LinearLayout top=horizontal();
        TextView back=button("‹",()->{
            if(scene==0)finish();else show(0);
        });
        top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(46)));
        LinearLayout label=vertical();label.setPadding(dp(12),0,0,0);
        add(label,text(pageTitle(),21,WHITE,true),0);
        add(label,text(today.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy",Locale.ENGLISH))+
            "  •  S Browser",12,CYAN,false),1);
        top.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        add(root,top,0);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        page=vertical();
        page.setPadding(dp(2),dp(10),dp(2),dp(28));
        scroll.addView(page);
        switch(scene){
            case 1:renderThought();break;
            case 2:renderSpeech();break;
            case 3:case 4:renderNews();break;
            case 5:renderAstrology();break;
            case 6:renderCrossword();break;
            case 7:renderPuzzles();break;
            case 8:renderAssemblyPack();break;
            default:renderHome();
        }
    }
    private void note(String value){add(page,text(value,12,MUTED,false),9);}
    private void header(String eyebrow,String title,String summary){
        LinearLayout box=vertical();
        box.setPadding(dp(17),dp(17),dp(17),dp(17));
        box.setBackground(background(0xff1e3554,0xff4a759e,23));
        add(box,text(eyebrow,12,MINT,true),0);
        add(box,text(title,23,WHITE,true),8);
        add(box,text(summary,13,MUTED,false),8);
        add(page,box,1);
    }
    private void actionRow(String left,Runnable a,String right,Runnable b){
        LinearLayout row=horizontal();
        TextView l=button(left,a),r=button(right,b);
        LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,dp(51),1);
        p1.rightMargin=dp(7);
        row.addView(l,p1);
        row.addView(r,new LinearLayout.LayoutParams(0,dp(51),1));
        add(page,row,10);
    }
    private void section(int n,String symbol,String name,String description,Runnable action){
        LinearLayout card=horizontal();
        card.setPadding(dp(14),dp(14),dp(10),dp(14));
        card.setBackground(background(PANEL,EDGE,20));
        TextView badge=text(n<10?"0"+n:String.valueOf(n),20,CYAN,true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(background(0xff1c3b55,0xff396f85,15));
        card.addView(badge,new LinearLayout.LayoutParams(dp(52),dp(60)));
        LinearLayout words=vertical();
        words.setPadding(dp(12),0,dp(5),0);
        add(words,text(symbol+"  "+name,17,WHITE,true),0);
        add(words,text(description,12,MUTED,false),6);
        card.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView next=text("›",27,MINT,true);
        card.addView(next,new LinearLayout.LayoutParams(dp(22),dp(47)));
        card.setOnClickListener(v->action.run());
        card.setContentDescription(name+" — "+description);
        add(page,card,10);
    }
    private void renderHome(){
        header("GOOD MORNING • SCHOOL ASSEMBLY",
            "One place for daily learning",
            "Short, point-wise content for students, teachers and morning assemblies.");
        DailyAssemblyContent.Thought t=DailyAssemblyContent.thought(today);
        TextView preview=text("TODAY'S THOUGHT\n"+
            (hindi?t.hindi:t.english),16,WHITE,true);
        preview.setPadding(dp(15),dp(14),dp(15),dp(14));
        preview.setBackground(background(0xff173c43,0xff3c887e,17));
        add(page,preview,12);
        TextView full=button("☰  Ready-to-read Assembly Script  →",()->show(8));
        full.setTextColor(GOLD);add(page,full,10);
        add(page,text("DAILY SECTIONS",13,MINT,true),22);
        section(1,"✦","Thought of the Day","Daily Hindi/English thought • Copy & Share",
            ()->show(1));
        section(2,"🎤","Speech","Fresh prepared Hindi/English speech each calendar day",
            ()->show(2));
        section(3,"⚽","Sports News","Latest available headlines • publisher & time",
            ()->show(3));
        section(4,"📰","General News","Trusted publisher headlines • confirm original source",
            ()->show(4));
        section(5,"✧","Astrology","Optional zodiac-themed positive reflection (for fun)",
            ()->show(5));
        section(6,"▦","Crossword","Interactive daily 4×4 word-square crossword",
            ()->show(6));
        section(7,"🧩","Puzzle Games","Word scramble and school riddle",
            ()->show(7));
        note("New thought and speech every calendar day (31 original items each). "+
            "News refreshes on opening, needs internet and is NOT independently fact-checked.");
    }
    private void chooseLanguage(){
        new AlertDialog.Builder(this).setTitle("Content language")
            .setItems(new String[]{"हिन्दी","English"},(d,i)->{
                hindi=i==0;remember();show(scene);
            }).setNegativeButton("Cancel",null).show();
    }
    private void renderThought(){
        DailyAssemblyContent.Thought t=DailyAssemblyContent.thought(today);
        header("01 • DAILY INSPIRATION","Thought of the Day",
            "Original educational thought, rotated automatically each day.");
        add(page,button(hindi?"हिन्दी  ▾":"English  ▾",this::chooseLanguage),14);
        TextView words=text(hindi?t.hindi:t.english,22,WHITE,true);
        words.setPadding(dp(19),dp(26),dp(19),dp(26));
        words.setBackground(background(0xff143f43,0xff4b9b87,24));
        add(page,words,12);
        String chosen=hindi?t.hindi:t.english;
        actionRow("Copy thought",()->copy(chosen),"Share thought",()->share(chosen));
        note("Thoughts are original S Browser assembly prompts, not falsely attributed quotes.");
    }
    private void speak(String content){
        if(speechEngine==null||!readyToSpeak){
            toast("Speech engine is not available on this device");return;
        }
        Locale locale=hindi?new Locale("hi","IN"):Locale.ENGLISH;
        int available=speechEngine.setLanguage(locale);
        if(available==TextToSpeech.LANG_MISSING_DATA||
            available==TextToSpeech.LANG_NOT_SUPPORTED){
            toast("This language voice is not installed in Android Text-to-Speech");return;
        }
        speechEngine.speak(content,TextToSpeech.QUEUE_FLUSH,null,"s-browser-speech");
    }
    private void renderSpeech(){
        DailyAssemblyContent.Speech item=DailyAssemblyContent.speech(today);
        header("02 • SCHOOL ASSEMBLY",
            hindi?item.hindiTitle:item.title,
            "Short, respectful speech suitable for school morning assembly.");
        add(page,button(hindi?"भाषा: हिन्दी  ▾":"Language: English  ▾",
            this::chooseLanguage),13);
        String speech=hindi?item.hindi:item.english;
        TextView words=text(speech,17,WHITE,false);
        words.setTextIsSelectable(true);
        words.setPadding(dp(18),dp(17),dp(18),dp(17));
        words.setBackground(background(PANEL,EDGE,20));
        add(page,words,12);
        actionRow("🔊 Listen",()->speak(speech),
            "Copy speech",()->copy(speech));
        add(page,button("Share speech  ↗",()->share(speech)),9);
        note("Prepared content, not an event-specific factual news bulletin. "+
            "Have a teacher review before delivering.");
    }

    private void renderNews(){
        boolean sports=scene==3;
        header(sports?"03 • SPORTS BULLETIN":"04 • CURRENT AFFAIRS",
            sports?"Sports News":"General & Latest News",
            "Only selected established publishers with published times. "+
                "A trusted publisher is NOT proof every claim is correct.");
        actionRow(hindi?"हिन्दी  ▾":"English  ▾",()->{
                hindi=!hindi;remember();show(scene);
            },cgNews?"Chhattisgarh  ▾":"All India  ▾",()->{
                cgNews=!cgNews;remember();show(scene);
            });
        actionRow(newsDays==1?"Last 24 hours  ▾":"Last 3 days  ▾",()->{
                newsDays=newsDays==1?3:1;show(scene);
            },"↻ Refresh headlines",this::fetchNews);
        pageStatus=text("Fetching publisher headlines…",13,GOLD,false);
        add(page,pageStatus,15);
        newsProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        newsProgress.setIndeterminate(true);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(3));
        p.topMargin=dp(6);page.addView(newsProgress,p);
        LinearLayout newsContainer=vertical();
        add(page,newsContainer,10);
        TextView official=text(sports?
            "Official results: check match organisers for confirmed scores.":
            "Government statements: verify on PIB and News On AIR.",12,GOLD,false);
        add(page,official,9);
        actionRow("News On AIR  ↗",()->openStory("https://www.newsonair.gov.in/"),
            "PIB official  ↗",()->openStory("https://pib.gov.in/"));
        final int selectedScene=scene,days=newsDays;
        final boolean lang=hindi,cg=cgNews;
        final String query=DailyNewsSources.feed(
            sports?DailyNewsSources.SPORTS:DailyNewsSources.BREAKING,lang,cg,days);
        final int request=++newsRequest;
        new Thread(()->{
            CityNewsFeed.Result result=null;String error=null;
            try{result=CityNewsFeed.fetchSchoolNewsRss(query,days);}
            catch(Exception e){error=e.getMessage();}
            final CityNewsFeed.Result data=result;
            final String problem=error;
            runOnUiThread(()->{
                if(closed||scene!=selectedScene||request!=newsRequest)return;
                newsProgress.setVisibility(View.GONE);
                newsContainer.removeAllViews();
                if(data==null){
                    pageStatus.setText("Live headlines unavailable — retry after checking internet");
                    add(newsContainer,text("Could not load recent news"+
                        (problem==null?"":": "+problem)+". No old or fabricated"+
                        " headlines have been substituted.",13,MUTED,false),8);
                }else{
                    pageStatus.setText(Math.min(6,data.stories.size())+
                        " curated headline(s) • Checked "+
                        DateFormat.getDateTimeInstance(DateFormat.SHORT,
                            DateFormat.SHORT).format(new Date(data.checkedAt)));
                    if(data.stories.isEmpty())
                        add(newsContainer,text("No recent articles matched the approved "+
                            "publisher list and date filter. Try a wider region or 3 days. "+
                            "S Browser will not invent a headline.",14,MUTED,false),10);
                    int shown=0;
                    for(CityNewsFeed.Story story:data.stories){
                        if(shown++>=6)break;
                        LinearLayout card=vertical();
                        card.setPadding(dp(14),dp(13),dp(14),dp(13));
                        card.setBackground(background(PANEL,EDGE,18));
                        TextView headline=text(story.title,16,WHITE,true);
                        headline.setMaxLines(4);add(card,headline,0);
                        String published=DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM,DateFormat.SHORT)
                            .format(new Date(story.published));
                        add(card,text("Publisher: "+story.source+"  •  "+published,
                            12,CYAN,false),8);
                        add(card,text("Publisher selected • Not independently fact-checked",
                            11,MUTED,false),7);
                        TextView read=text("Read original article  ↗",13,MINT,true);
                        add(card,read,8);
                        TextView cross=text("Compare coverage from other news sources  ↗",
                            12,CYAN,false);
                        add(card,cross,7);
                        cross.setOnClickListener(v->openStory(
                            NewsQualityGate.searchForCorroboration(story.title)));
                        card.setOnClickListener(v->openStory(story.url));
                        add(newsContainer,card,9);
                    }
                }
            });
        },"SBrowser-AssemblyNews").start();
        note("Only recent dated headlines from a limited list of established HTTPS "+
            "publishers are displayed. Their claims are not independently verified. "+
            "Open the article and compare coverage before using it at school.");
    }
    private void fetchNews(){
        if(scene==3||scene==4)show(scene);
    }
    private void openStory(String link){
        if(!CityNewsFeed.isWebUrl(link)){toast("Invalid article link");return;}
        Intent view=new Intent(Intent.ACTION_VIEW,Uri.parse(link),this,MainActivity.class);
        view.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(view);
    }

    private void renderAstrology(){
        header("05 • OPTIONAL, JUST FOR FUN",
            "Zodiac Reflection",
            "Traditional zodiac themes for entertainment and positive reflection.");
        add(page,text("Select a zodiac sign (no birth date or personal information required):",
            14,MUTED,false),16);
        TextView choose=button(DailyAssemblyContent.SIGNS[zodiac]+"  ▾",
            ()->new AlertDialog.Builder(this).setTitle("Choose a sign • for fun")
                .setItems(DailyAssemblyContent.SIGNS,(dialog,index)->{
                    zodiac=index;remember();show(5);
                }).show());
        add(page,choose,12);
        TextView reflection=text(DailyAssemblyContent.reflection(today,zodiac),
            20,WHITE,true);
        reflection.setPadding(dp(18),dp(23),dp(18),dp(23));
        reflection.setBackground(background(0xff29284f,0xff6a5b98,22));
        add(page,reflection,16);
        note("Entertainment only: these are not predictions, scientific findings, "+
            "or advice about health, money, exams or life decisions.");
    }

    private void renderCrossword(){
        DailyAssemblyContent.Crossword crossword=DailyAssemblyContent.crossword(today);
        header("06 • DAILY MINI CROSSWORD","4×4 Word-Square Crossword",
            "Fill the shared letters. Each row reads across and each column reads down.");
        add(page,text("Today's word puzzle • "+today,13,MUTED,false),14);
        LinearLayout board=vertical();
        board.setGravity(Gravity.CENTER);
        int width=Math.min(61,(int)(
            getResources().getDisplayMetrics().widthPixels/
            getResources().getDisplayMetrics().density-65)/4);
        cells=new EditText[crossword.size()*crossword.size()];
        String persisted=getSharedPreferences(PREFS,MODE_PRIVATE).getString(
            "cw_"+today.toString(),"");
        for(int r=0;r<crossword.size();r++){
            LinearLayout line=horizontal();line.setGravity(Gravity.CENTER);
            for(int c=0;c<crossword.size();c++){
                final int idx=r*crossword.size()+c;
                EditText cell=new EditText(this);
                cell.setSingleLine(true);
                cell.setTextSize(22);cell.setTextColor(WHITE);
                cell.setGravity(Gravity.CENTER);
                cell.setSelectAllOnFocus(true);
                cell.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
                cell.setInputType(InputType.TYPE_CLASS_TEXT|
                    InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                cell.setBackground(background(0xff1e3c5b,0xff537dab,10));
                if(persisted.length()==crossword.size()*crossword.size() &&
                    persisted.charAt(idx)!='_')cell.setText(
                        String.valueOf(persisted.charAt(idx)));
                cells[idx]=cell;
                LinearLayout.LayoutParams item=new LinearLayout.LayoutParams(
                    dp(width),dp(width));
                item.setMargins(dp(3),dp(3),dp(3),dp(3));
                line.addView(cell,item);
                cell.addTextChangedListener(new TextWatcher(){
                    public void beforeTextChanged(CharSequence s,int st,int n,int a){}
                    public void onTextChanged(CharSequence s,int st,int before,int count){
                        saveCrossword();
                        if(s.length()==1&&idx<cells.length-1 &&
                            cells[idx+1]!=null)cells[idx+1].requestFocus();
                    }
                    public void afterTextChanged(Editable s){}
                });
            }
            add(board,line,1);
        }
        add(page,board,13);
        add(page,text("ACROSS →",13,MINT,true),16);
        for(String clue:crossword.across)add(page,text(clue,14,WHITE,false),4);
        add(page,text("DOWN ↓",13,MINT,true),16);
        for(String clue:crossword.down)add(page,text(clue,14,WHITE,false),4);
        actionRow("✓ Check answers",()->{
            String[] typed=new String[cells.length];
            int filled=0;
            for(int i=0;i<cells.length;i++){
                typed[i]=cells[i].getText().toString();
                if(!typed[i].trim().isEmpty())filled++;
            }
            if(crossword.correct(typed))toast("Excellent! Crossword complete ✓");
            else toast("Keep trying! "+filled+" of "+cells.length+" letters entered.");
        },"💡 One hint",()->{
            for(int i=0;i<cells.length;i++){
                String entered=cells[i].getText().toString().trim();
                char expected=crossword.cell(i/crossword.size(),i%crossword.size());
                if(entered.isEmpty()||Character.toUpperCase(entered.charAt(0))!=expected){
                    cells[i].setText(String.valueOf(expected));
                    cells[i].setTextColor(GOLD);saveCrossword();
                    toast("One letter revealed");return;
                }
            }
            toast("All letters filled correctly!");
        });
        add(page,button("↺ Clear crossword",()->{
            for(EditText e:cells)e.setText("");
            saveCrossword();
        }),9);
        note("This is a compact word-square crossword: across and down answers "+
            "share the same letters. Progress stays on your phone for today.");
    }
    private void saveCrossword(){
        if(cells==null)return;
        StringBuilder value=new StringBuilder();
        for(EditText field:cells){
            if(field==null)return;
            String s=field.getText().toString().trim();
            value.append(s.isEmpty()?'_':Character.toUpperCase(s.charAt(0)));
        }
        getSharedPreferences(PREFS,MODE_PRIVATE).edit()
            .putString("cw_"+today.toString(),value.toString()).apply();
    }
    private void renderPuzzles(){
        LocalDate day=today.plusDays(practiceOffset);
        DailyAssemblyContent.Puzzle puzzle=DailyAssemblyContent.puzzle(day);
        header("07 • BRAIN GAMES","Daily Puzzle Games",
            "A short word scramble and riddle to warm up the mind.");
        add(page,text("WORD SCRAMBLE",13,MINT,true),18);
        TextView scrambled=text(puzzle.scrambled,27,WHITE,true);
        scrambled.setGravity(Gravity.CENTER);
        scrambled.setPadding(dp(10),dp(19),dp(10),dp(19));
        scrambled.setBackground(background(PANEL,EDGE,21));
        add(page,scrambled,9);
        EditText answer=new EditText(this);
        answer.setSingleLine(true);answer.setHint("Enter the correct word");
        answer.setTextColor(WHITE);answer.setHintTextColor(MUTED);
        answer.setTextSize(18);
        answer.setInputType(InputType.TYPE_CLASS_TEXT|
            InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        answer.setPadding(dp(14),dp(10),dp(14),dp(10));
        answer.setBackground(background(PANEL,EDGE,15));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(57));
        ap.topMargin=dp(11);page.addView(answer,ap);
        actionRow("✓ Check word",()->{
            if(DailyAssemblyContent.answer(answer.getText().toString(),puzzle.answer))
                toast("Correct! Well done ✓");
            else toast("Not quite yet. Try the hint!");
        },"💡 Hint",()->toast(puzzle.hint));
        add(page,text("RIDDLE OF THE DAY",13,MINT,true),22);
        TextView riddle=text(puzzle.riddle,18,WHITE,false);
        riddle.setPadding(dp(16),dp(19),dp(16),dp(19));
        riddle.setBackground(background(PANEL,EDGE,18));
        add(page,riddle,9);
        add(page,button("Show riddle answer",()->
            new AlertDialog.Builder(this).setTitle("Riddle answer")
                .setMessage(puzzle.riddleAnswer)
                .setPositiveButton("Got it",null).show()),9);
        add(page,button("↻ Practice another word",()->{
            practiceOffset++;show(7);
        }),11);
        note("Games are playable offline. No scores, rankings, ads or data collection.");
    }

    private void renderAssemblyPack(){
        header("READY-TO-READ","Morning Assembly Script",
            "A short thought, speech and riddle in a clear point-wise order.");
        add(page,button(hindi?"Language: हिन्दी  ▾":"Language: English  ▾",
            this::chooseLanguage),13);
        String pack=DailyAssemblyContent.assemblyPlan(today,hindi);
        TextView script=text(pack,16,WHITE,false);
        script.setTextIsSelectable(true);
        script.setPadding(dp(17),dp(17),dp(17),dp(17));
        script.setBackground(background(PANEL,EDGE,20));
        add(page,script,12);
        actionRow("Copy script",()->copy(pack),"Share script",()->share(pack));
        note("Live sports/breaking stories are deliberately NOT inserted"+
            " as facts without a current attributed source. Check those sections before reading news aloud.");
    }
    @Override protected void onResume(){
        super.onResume();
        LocalDate current=LocalDate.now(ZoneId.systemDefault());
        if(!current.equals(today)){
            today=current;
            practiceOffset=0;
            show(scene);
        }
    }
    @Override public void onBackPressed(){if(scene==0)super.onBackPressed();else show(0);}
    @Override protected void onDestroy(){
        closed=true;newsRequest++;
        if(speechEngine!=null){speechEngine.stop();speechEngine.shutdown();}
        super.onDestroy();
    }
}
