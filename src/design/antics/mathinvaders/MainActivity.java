package design.antics.mathinvaders;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
    private GameView game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        game = new GameView(this);
        setContentView(game);
        game.onMenu = () -> {
            game.menuOpened();
            ParentsMenu.open(this, new ParentsMenu.Host() {
                public String[] packNames() { return Packs.NAMES; }
                public String[] packExamples() { return Packs.EXAMPLES; }
                public int packs() { return game.packs(); }
                public void setPacks(int m) { game.setPacks(m); }
                public int speed() { return game.speed(); }
                public void setSpeed(int p) { game.setSpeed(p); }
                public void resetBest() { game.resetBest(); }
                public void onMenuClosed() { hideSystemUI(); }
            });
        };
        hideSystemUI();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override protected void onResume() { super.onResume(); hideSystemUI(); game.resume(); }
    @Override protected void onPause() { game.pause(); super.onPause(); }
}
