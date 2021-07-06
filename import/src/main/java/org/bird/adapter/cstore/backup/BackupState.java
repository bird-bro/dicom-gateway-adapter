package org.bird.adapter.cstore.backup;

/**
 * @author bird
 * @date 2021-7-2 17:45
 **/
public class BackupState {

    private String uniqueFileName;
    private int attemptsCountdown = 1;


    public BackupState(String uniqueFileName, int attemptsCountdown) {
        this.uniqueFileName = uniqueFileName;
        this.attemptsCountdown += attemptsCountdown;
    }

    public String getUniqueFileName() {
        return uniqueFileName;
    }

    public int getAttemptsCountdown() {
        return attemptsCountdown;
    }


    /**
     * Decrements attemptsCountdown field value if it`s value more then zero.
     * @return true if decremented, false if not.
     * @since 2021-3-2 13:22
     */
    public boolean decrement() {
        if (attemptsCountdown > 0) {
            attemptsCountdown--;
            return true;
        }
        return false;
    }

}
