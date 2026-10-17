/*
 * Su Royal Arena: card art renderer for Blockbench.
 *
 * Renders the open model as a card portrait for the mod's HUD: the card's
 * 31:39 shape, the subject framed the same way on every card, a card
 * background, and a PNG named after the card id, ready for
 * assets/royalarena/textures/gui/card/<card id>.png.
 *
 * Install: File > Plugins > Load Plugin from File, pick this file.
 * Use: Tools > "Render Card Art", or the camera button in the toolbar menu.
 * See model_sources/card_art/README.md.
 */
(function () {
    'use strict';

    const PLUGIN_ID = 'royalarena_card_art';

    /**
     * Card id -> elixir cost, in BattleCard order. DomainSmokeTest checks
     * that this matches the cards in the mod.
     */
    const CARDS = {
        'zombie': 3,
        'skeleton_archer': 2,
        'spider': 3,
        'skeletons': 2,
        'big_slime': 3,
        'stray': 2,
        'vindicator': 2,
        'iron_golem': 5,
        'creeper': 3,
        'witch': 4,
        'wolves': 3,
        'ravager': 5,
        'hoglin': 3,
        'pillager': 3,
        'magma_cube': 4,
        'arrow_storm': 3,
        'fireball': 4,
        'lightning': 6,
        'healing_potion': 3,
        'freeze': 4,
        'tnt': 5,
    };
    const CARD_IDS = Object.keys(CARDS);

    /** Card face in reference units (HudLayout.CARD_W x CARD_H). */
    const CARD_W = 31;
    const CARD_H = 39;
    /** Colours from BattleHudOverlay. */
    const CARD_FILL = '#3b5b8c';
    const CARD_EDGE = '#1e2433';
    const ELIXIR = '#d05cff';
    const ELIXIR_EDGE = '#1a0e1f';

    const SIZES = {
        small: [124, 156],
        normal: [248, 312],
        large: [496, 624],
    };

    let action;
    let lastSettings = {
        card: CARD_IDS[0],
        size: 'normal',
        angle: 'fixed',
        yaw: 30,
        pitch: 15,
        fov: 30,
        zoom: 1.0,
        lift: 0,
        background: 'gradient',
        color: CARD_FILL,
    };

    // ------------------------------------------------------------ camera

    /** Unit vector from the subject to the camera. */
    function viewDirection(settings) {
        if (settings.angle === 'current' && Preview.selected) {
            const preview = Preview.selected;
            const target = preview.controls && preview.controls.target ?
                preview.controls.target : new THREE.Vector3();
            const dir = preview.camera.position.clone().sub(target);
            if (dir.lengthSq() > 1e-6)
                return dir.normalize();
        }
        const yaw = THREE.MathUtils.degToRad(settings.yaw);
        const pitch = THREE.MathUtils.degToRad(settings.pitch);
        return new THREE.Vector3(
            Math.sin(yaw) * Math.cos(pitch),
            Math.sin(pitch),
            Math.cos(yaw) * Math.cos(pitch)).normalize();
    }

    /**
     * Places the camera so the subject's bounding sphere fits the card with
     * the same margin on every card: the sphere touches the tighter of the
     * two card edges at zoom 1.
     */
    function frameCamera(camera, box, settings) {
        const sphere = box.getBoundingSphere(new THREE.Sphere());
        const vertical = THREE.MathUtils.degToRad(settings.fov) / 2;
        const horizontal = Math.atan(Math.tan(vertical) * CARD_W / CARD_H);
        const fit = Math.max(sphere.radius / Math.sin(vertical),
            sphere.radius / Math.sin(horizontal));
        const distance = fit / Math.max(0.1, settings.zoom);
        const target = sphere.center.clone();
        // A positive lift moves the subject up on the card.
        target.y -= settings.lift * sphere.radius / 10;
        camera.position.copy(target).add(
            viewDirection(settings).multiplyScalar(distance));
        camera.near = Math.max(0.1, distance - sphere.radius * 4);
        camera.far = distance + sphere.radius * 4;
        camera.lookAt(target);
        camera.updateProjectionMatrix();
    }

    // ------------------------------------------------------------ render

    /** A copy of the model without selection outlines and helpers. */
    function cloneModel() {
        const root = Project && Project.model_3d ? Project.model_3d : null;
        if (!root)
            throw new Error('No model is open.');
        const copy = root.clone(true);
        copy.traverse(object => {
            if (object.isLine || object.isLineSegments || object.isPoints)
                object.visible = false;
        });
        copy.updateMatrixWorld(true);
        return copy;
    }

    function visibleBox(object) {
        const box = new THREE.Box3();
        object.traverse(child => {
            if (child.isMesh && child.visible && child.geometry) {
                child.geometry.computeBoundingBox();
                const part = child.geometry.boundingBox.clone()
                    .applyMatrix4(child.matrixWorld);
                box.union(part);
            }
        });
        if (box.isEmpty())
            throw new Error('The model has no visible elements.');
        return box;
    }

    /** Renders the subject alone on a transparent canvas. */
    function renderSubject(settings, width, height) {
        const scene = new THREE.Scene();
        const model = cloneModel();
        scene.add(model);
        scene.add(new THREE.AmbientLight(0xffffff, 1));
        const camera = new THREE.PerspectiveCamera(
            settings.fov, width / height, 0.1, 1000);
        frameCamera(camera, visibleBox(model), settings);

        const renderer = new THREE.WebGLRenderer({
            alpha: true, antialias: true, preserveDrawingBuffer: true,
        });
        try {
            renderer.setPixelRatio(1);
            renderer.setSize(width, height, false);
            renderer.setClearColor(0x000000, 0);
            renderer.render(scene, camera);
            const out = document.createElement('canvas');
            out.width = width;
            out.height = height;
            out.getContext('2d').drawImage(renderer.domElement, 0, 0);
            return out;
        } finally {
            renderer.dispose();
            if (renderer.forceContextLoss)
                renderer.forceContextLoss();
        }
    }

    function shade(hex, amount) {
        const n = parseInt(hex.replace('#', ''), 16);
        const c = [n >> 16 & 255, n >> 8 & 255, n & 255].map(v =>
            Math.max(0, Math.min(255, Math.round(v + amount * 255))));
        return 'rgb(' + c.join(',') + ')';
    }

    /** The finished card art: background plus subject, no frame. */
    function composeCard(settings) {
        const [width, height] = SIZES[settings.size] || SIZES.normal;
        const subject = renderSubject(settings, width, height);
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        const base = settings.background === 'color' ? settings.color :
            CARD_FILL;
        if (settings.background === 'gradient') {
            const g = ctx.createRadialGradient(width / 2, height * 0.4,
                width * 0.1, width / 2, height * 0.45, height * 0.75);
            g.addColorStop(0, shade(base, 0.22));
            g.addColorStop(1, shade(base, -0.12));
            ctx.fillStyle = g;
            ctx.fillRect(0, 0, width, height);
        } else if (settings.background !== 'transparent') {
            ctx.fillStyle = base;
            ctx.fillRect(0, 0, width, height);
        }
        ctx.drawImage(subject, 0, 0);
        return canvas;
    }

    /**
     * How the art looks on a hand card in the game: the card edge and the
     * cost badge on the top-left corner (preview only, not exported).
     */
    function mockup(card, cost) {
        const scale = card.width / CARD_W;
        const edge = Math.max(2, Math.round(1.5 * scale));
        const badge = 11 * scale;
        const overhang = 2.5 * scale;
        const canvas = document.createElement('canvas');
        canvas.width = Math.round(card.width + overhang * 2);
        canvas.height = Math.round(card.height + overhang * 2);
        const ctx = canvas.getContext('2d');
        ctx.translate(overhang, overhang);
        ctx.fillStyle = CARD_EDGE;
        ctx.fillRect(0, 0, card.width, card.height);
        ctx.drawImage(card, edge, edge, card.width - edge * 2,
            card.height - edge * 2);
        ctx.fillStyle = ELIXIR_EDGE;
        ctx.fillRect(-overhang, -overhang, badge, badge);
        ctx.fillStyle = ELIXIR;
        ctx.fillRect(-overhang + 2, -overhang + 2, badge - 4, badge - 4);
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold ' + Math.round(badge * 0.7) + 'px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(cost, -overhang + badge / 2, -overhang + badge / 2 + 1);
        return canvas;
    }

    // ------------------------------------------------------------ dialogs

    function save(card, settings) {
        Blockbench.export({
            resource_id: 'royalarena_card_art',
            type: 'PNG',
            extensions: ['png'],
            name: settings.card,
            content: card.toDataURL('image/png'),
            savetype: 'image',
        });
    }

    function showResult(settings) {
        let card;
        try {
            card = composeCard(settings);
        } catch (error) {
            Blockbench.showMessageBox({
                title: 'Render Card Art', message: String(error.message || error),
            });
            return;
        }
        const preview = mockup(card, String(CARDS[settings.card]));
        new Dialog({
            id: PLUGIN_ID + '_result',
            title: 'Card art: ' + settings.card + '.png',
            lines: [
                '<div style="display:flex;gap:16px;align-items:flex-start">' +
                '<div><p>Exported image</p><img style="image-rendering:pixelated;' +
                'max-height:340px" src="' + card.toDataURL() + '"></div>' +
                '<div><p>In the hand (preview)</p><img style="max-height:340px" ' +
                'src="' + preview.toDataURL() + '"></div></div>',
            ],
            buttons: ['Save PNG', 'Change settings', 'Cancel'],
            confirmIndex: 0,
            cancelIndex: 2,
            onButton(index) {
                if (index === 0)
                    save(card, settings);
                else if (index === 1)
                    setTimeout(() => showSettings(), 0);
            },
        }).show();
    }

    function showSettings() {
        const cards = {};
        for (const id of CARD_IDS)
            cards[id] = id;
        new Dialog({
            id: PLUGIN_ID + '_settings',
            title: 'Render Card Art',
            width: 420,
            form: {
                card: {label: 'Card', type: 'select', options: cards,
                    value: lastSettings.card},
                size: {label: 'Size', type: 'select', value: lastSettings.size,
                    options: {small: '124 x 156', normal: '248 x 312',
                        large: '496 x 624'}},
                angle: {label: 'Angle', type: 'select', value: lastSettings.angle,
                    options: {fixed: 'Fixed (yaw / pitch below)',
                        current: 'Current view'}},
                yaw: {label: 'Yaw', type: 'number', value: lastSettings.yaw,
                    min: -360, max: 360, step: 5},
                pitch: {label: 'Pitch', type: 'number', value: lastSettings.pitch,
                    min: -89, max: 89, step: 5},
                fov: {label: 'Field of view', type: 'number',
                    value: lastSettings.fov, min: 5, max: 90},
                zoom: {label: 'Zoom', type: 'number', value: lastSettings.zoom,
                    min: 0.3, max: 4, step: 0.05},
                lift: {label: 'Move subject up', type: 'number',
                    value: lastSettings.lift, min: -10, max: 10, step: 0.5},
                background: {label: 'Background', type: 'select',
                    value: lastSettings.background,
                    options: {gradient: 'Card blue, soft light',
                        flat: 'Card blue, flat', color: 'Custom colour',
                        transparent: 'Transparent'}},
                color: {label: 'Custom colour', type: 'color',
                    value: lastSettings.color},
            },
            onConfirm(result) {
                const color = result.color && result.color.toHexString ?
                    result.color.toHexString() : (result.color || CARD_FILL);
                lastSettings = Object.assign({}, lastSettings, result,
                    {color: color});
                setTimeout(() => showResult(lastSettings), 0);
            },
        }).show();
    }

    // ------------------------------------------------------------ plugin

    Plugin.register(PLUGIN_ID, {
        title: 'Su Royal Arena Card Art',
        author: 'susu609',
        description: 'Renders the open model as a Su Royal Arena card ' +
            'portrait (31:39, card background, named after the card id).',
        icon: 'style',
        version: '1.0.0',
        variant: 'both',
        onload() {
            action = new Action('royalarena_render_card_art', {
                name: 'Render Card Art',
                description: 'Render the model as a Su Royal Arena card image',
                icon: 'style',
                click: showSettings,
            });
            MenuBar.addAction(action, 'tools');
        },
        onunload() {
            if (action)
                action.delete();
        },
    });
})();
