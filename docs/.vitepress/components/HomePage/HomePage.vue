<template>
    <section class="content">
        <div class="container other-color">
            <div class="panels vp-doc">
                <div class="panel">
                    <h2>Get Started in Seconds</h2>
                    <p>Set up Launch0 and take control of your home screen</p>
                    <div class="steps">
                        <slot name="setup-steps" />
                    </div>
                </div>
                <div class="panel">
                    <h2>Powerful Gestures</h2>
                    <p>Navigate your phone faster with intuitive gestures</p>
                    <div class="steps">
                        <slot name="gestures" />
                    </div>
                </div>
            </div>
            <div class="features-list vp-doc">
                <h2>Everything You Need</h2>
                <p>A focused set of features — nothing more, nothing less</p>
                <div class="content">
                    <slot name="features-list" />
                </div>
            </div>
        </div>
        <div class="container">
            <div class="panels vp-doc">
                <div class="panel">
                    <h2>Notes &amp; To-Dos</h2>
                    <p>A private chat with yourself, one swipe from home</p>
                    <div class="steps">
                        <slot name="notes" />
                    </div>
                </div>
                <div class="panel">
                    <h2>Do Not Disturb</h2>
                    <p>Hold notifications from distracting apps until you're ready</p>
                    <div class="steps">
                        <slot name="do-not-disturb" />
                    </div>
                </div>
            </div>
        </div>
        <div class="container other-color">
            <div class="panels vp-doc">
                <div class="panel">
                    <h2>Screen Time Awareness</h2>
                    <p>Stay informed about your phone usage — right on your home screen</p>
                    <div class="steps">
                        <slot name="screen-time" />
                    </div>
                </div>
                <div class="panel">
                    <h2>Auto-Generated Wallpapers</h2>
                    <p>A fresh look every hour, generated right on your device</p>
                    <div class="steps">
                        <slot name="wallpapers" />
                    </div>
                </div>
            </div>
        </div>
        <div class="container">
            <div class="panels vp-doc">
                <div class="panel center-panel">
                    <h2>Privacy as a Feature</h2>
                    <p>Launch0 is fully open source and collects zero data. No analytics, no tracking, no ads — ever.</p>
                    <div class="steps">
                        <slot name="privacy" />
                    </div>
                </div>
            </div>
        </div>
    </section>
</template>

<style scoped>
.content {
    margin-top: calc(96px * 1.5);
    display: flex;
    flex-direction: column;

    .container {
        position: relative;
        isolation: isolate;

        &.other-color {
            margin-bottom: calc(96px * 1.5 - (24px + 16px));

            &:before {
                content: "";
                position: absolute;
                inset: calc(-128px - 24px) var(--vp-offset, calc(-1 * (100vw - 100%) / 2));
                display: block;
                z-index: -1;
                pointer-events: none;
                --gradient-size: 128px;
                /* Translucent tint so the starfield behind stays visible */
                --band-color: rgba(255, 255, 255, 0.03);
                background-image: linear-gradient(to bottom, transparent, var(--band-color) var(--gradient-size), var(--band-color) calc(100% - var(--gradient-size)), transparent);
            }
        }
    }
}

.panels {
    display: flex;
    flex-direction: column;
    column-gap: calc(16px + (24px * 2));
    row-gap: 24px;
    padding: 24px 0px;

    @media (min-width: 960px) {
        display: flex;
        flex-direction: row !important;
        flex-wrap: wrap;
    }

    > .panel {
        flex-grow: 1;
        border-radius: 12px;
        flex-basis: 0%;

        &.center-panel {
            text-align: center;
            max-width: 640px;
            margin: 0 auto;
        }

        > h2 {
            letter-spacing: -0.02em;
            line-height: 40px;
            font-size: 28px;
            margin: 16px 0px;
            padding-top: 0px;
            border-top: none;

            @media (min-width: 768px) {
                font-size: 32px;
            }
        }
    }
}

.features-list {
    padding: 0px calc((100svw - 100%) / 2);
    margin: 32px calc(-1*(100svw - 100%) / 2) 0px calc(-1*(100svw - 100%) / 2);
    overflow: hidden;

    > h2 {
        letter-spacing: -0.02em;
        line-height: 40px;
        font-size: 28px;
        margin: 16px 0px;
        padding-top: 0px;
        border-top: none;

        @media (min-width: 768px) {
            font-size: 32px;
        }
    }

    > .content {
        overflow: hidden;
        --mask-size: 148px;
        --fade-size: 64px;
        --fade-offset: 48px;
        margin: 0px calc(-1 * var(--mask-size, 0px));
        padding: 0px var(--mask-size, 0px);
        margin-top: 24px;

        @media (min-width: 1280px) {
            --fade-size: 96px;
            --fade-offset: 48px;
        }

        &:deep(>ul) {
            display: flex;
            flex-direction: row;
            flex-wrap: wrap;
            padding: 0px;
            margin: 0px calc(-1 * var(--mask-size, 0px));
            justify-content: center;
            gap: 12px;
            mask: linear-gradient(to right, transparent calc(var(--mask-size) - var(--fade-offset)), black calc(var(--mask-size) + var(--fade-size)), black calc(100% - var(--mask-size) - var(--fade-size)), transparent calc(100% - var(--mask-size) + var(--fade-offset)));
            margin-bottom: calc(-1lh - (12px * 2) - 6px);

            &:global(>li) {
                display: block;
                padding: 12px 16px;
                border-radius: 12px;
                background-color: var(--vp-c-bg-soft);
                margin: 0px;
                white-space: pre;

                &:global(>a) {
                    margin: -13px -17px;
                    padding: 12px 16px;
                    border-radius: 12px;
                    text-decoration: none;
                    color: inherit;
                    font-weight: inherit;
                    border: solid 1px transparent;
                    transition: border-color 0.25s;

                    &:hover {
                        border-color: var(--vp-c-brand-1);
                    }
                }
            }
        }
    }
}

</style>
