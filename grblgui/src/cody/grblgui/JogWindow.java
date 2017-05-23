package cody.grblgui;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

public class JogWindow extends Window {
	MainScreen mainscreen;
	
	TextField x;
	TextField y;
	TextField z;
	TextField step;
	CheckBox current;
	TextButton set;
	TextButton zero;
	
	@Override
	public void draw(Batch arg0, float arg1) {
		super.draw(arg0, arg1);
		if(mainscreen.grbl == null)
			return;
		Vector3 pos = mainscreen.grbl.getToolPosition();
		set.setVisible(!current.isChecked());
		if(current.isChecked()) {
		String tmp = Float.toString(pos.x);
		if(!tmp.equals(x.getText()))
			x.setText(tmp);
		tmp = Float.toString(pos.y);
		if(!tmp.equals(y.getText()))
			y.setText(tmp);
		tmp = Float.toString(pos.z);
		if(!tmp.equals(z.getText()))
			z.setText(tmp);
		}
	}
	
	int getStep() {
		return Integer.parseInt(step.getText());
	}
	float getXpos() {
		return Float.parseFloat(x.getText());
	}
	float getYpos() {
		return Float.parseFloat(y.getText());
	}
	float getZpos() {
		return Float.parseFloat(z.getText());
	}


	void jog(float x, float y, float z) {
		if(mainscreen.grbl == null)
			return;
		if(mainscreen.grbl.isStreaming())
			return;
		int step = getStep();
		
		mainscreen.grbl.jogStart(step, x, y, z);
		//grbl.send(("G1" + "\n").getBytes());
		
	}
	void jogStop() {
		if(mainscreen.grbl == null)
			return;
		if(mainscreen.grbl.isStreaming())
			return;
		mainscreen.grbl.jogCancel();
	}

	void go() {
		if(mainscreen.grbl == null)
			return;
		if(mainscreen.grbl.isStreaming())
			return;
		mainscreen.grbl.send(("G0X" + getXpos() + "Y" + getYpos() + "Z" + getZpos() + "\n").getBytes());
		//grbl.send(("G1" + "\n").getBytes());
		
	}
	
	public JogWindow(Skin skin, MainScreen _mainscreen) {
		super("Jog", skin);
		mainscreen = _mainscreen;
		
		setBounds(0, 0, 200, 400);
		setColor(1, 0, 0, 0.8f);

		Table table3 = new Table();
		add(table3).expand().fill();
		row();
		
		current = new CheckBox("Current", skin);
		current.setChecked(true);
		table3.add(current).fill().expand();
		set = new TextButton("Move", skin);
		table3.add(set).fill().expand();
		set.addListener(
            	new InputListener() {
            		@Override
            	public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                		go();
    				return true;
            	}});
		zero = new TextButton("Set Zero", skin);
		zero.addListener(
            	new InputListener() {
            		@Override
            	public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
            			if(mainscreen.grbl == null)
            				return true;
                		if(!mainscreen.grbl.isStreaming()) {
                			//Vector3 v = grbl.machinePosition.cpy().sub(grbl.toolPosition.cpy());
                			Vector3 v = mainscreen.grbl.getMachinePosition().cpy();
                			mainscreen.grbl.send(("G10 L2 P1 X" + v.x + " Y" + v.y +" Z" + v.z + "\n").getBytes());
                		}
    				return true;
            	}});
		table3.add(zero).fill().expand();
		
		Table table1 = new Table();
		add(table1).expand().fill();
		row();
		
		table1.add(new Label("Speed", skin)).fill().expand();
		table1.add(step = new TextField("100", skin)).fill().expand();
		table1.row();
		table1.add(new Label("X", skin)).fill().expand();
		table1.add(x = new TextField("0.000", skin)).fill().expand();
		table1.row();
		table1.add(new Label("Y", skin)).fill().expand();
		table1.add(y = new TextField("0.000", skin)).fill().expand();
		table1.row();
		table1.add(new Label("Z", skin)).fill().expand();
		table1.add(z = new TextField("0.000", skin)).fill().expand();

		Table table2 = new Table();
		add(table2).expand().fill();
		row();
		
		final TextButton up = new TextButton("Z+", skin);
		table2.add(up).fill().expand();
		up.addListener(
            	new InputListener() {
            		@Override
            	public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                		jog(0,0,1);
    				return true;
            	}
            		@Override
            	public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                		jogStop();
            	}});
		
		final TextButton yp = new TextButton("Y+", skin);
		table2.add(yp).fill().expand();
		yp.addListener(
            	new InputListener() {
            		@Override
            	public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                		jog(0,1,0);
    				return true;
            	}
            		@Override
            	public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                		jogStop();
            	}
            		}
            	);
		
		table2.add(new Label("", skin)).fill().expand();
		table2.row();
		
		final TextButton xp = new TextButton("X-", skin);
		table2.add(xp).fill().expand();
		xp.addListener(
            	new InputListener() {
            		@Override
            	public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                		jog(-1,0,0);
    				return true;
            	}
            		@Override
            	public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                		jogStop();
            	}});
		
		table2.add(new Label("", skin)).fill().expand();
		
		final TextButton xm = new TextButton("X+", skin);
		table2.add(xm).fill().expand();
		xm.addListener(
            	new InputListener() {
            		@Override
            	public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                		jog(1,0,0);
    				return true;
            	}
            		@Override
            	public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                		jogStop();
            	}});
		
		table2.row();
		
		final TextButton zm = new TextButton("Z-", skin);
		table2.add(zm).fill().expand();
		zm.addListener(
            	new InputListener() {
            		@Override
            	public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                		jog(0,0,-1);
    				return true;
            	}
            		@Override
            	public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                		jogStop();
            	}});
		
		final TextButton ym = new TextButton("Y-", skin);
		table2.add(ym).fill().expand();
		ym.addListener(
            	new InputListener() {
            		@Override
            	public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                		jog(0,-1,0);
    				return true;
            	}
            		@Override
            	public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                		jogStop();
            	}});
	}

}
